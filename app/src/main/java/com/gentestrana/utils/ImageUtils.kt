package com.gentestrana.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import android.util.Log
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.security.MessageDigest
import android.graphics.Bitmap
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.core.content.FileProvider
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap


/**
 * Uploads the profile image to Firebase Storage and returns the download URL via onComplete.
 * @param uid The user's unique ID.
 * @param imageUri The URI of the selected image.
 * @param onComplete Callback that returns the download URL if successful, or an empty string on failure.
 */
fun uploadMainProfileImage(
    context: Context,
    uid: String,
    imageUri: Uri,
    onComplete: (String) -> Unit
) {
    Log.d("ImageUpload", "uploadMainProfileImage STARTED - uid: $uid, imageUri: $imageUri")

    // Controllo rate limiter per upload
    if (!UploadRateLimiter.canUpload(uid)) {
        Log.d("ImageUpload", "Upload rate limit exceeded for user: $uid")
        onComplete("LIMIT_EXCEEDED")
        return
    }

    // Conversione a WebP se possibile per risparmiare spazio e banda
    val webpBytes = convertImageUriToWebP(context, imageUri)
    val uploadUri: Uri
    if (webpBytes != null) {
        val tempFileName = "main_profile_${System.currentTimeMillis()}.webp"
        val tempUri = saveByteArrayToTempFile(context, webpBytes, tempFileName)
        uploadUri = tempUri ?: imageUri
    } else {
        uploadUri = imageUri
    }

    // 1. Genera l'hash MD5 dall'URI dell'immagine
    val md5Hash = generateMD5HashFromUri(context, uploadUri)
    val filename = if (md5Hash != null) {
        "$md5Hash.webp"  // Usa l'hash MD5 come nome del file
    } else {
        // Fallback: usa un timestamp se l'hash non è disponibile
        Log.w("ImageUpload", "MD5 hash non disponibile, uso timestamp come fallback")
        "${System.currentTimeMillis()}.webp"
    }

    Log.d("ImageUpload", "Nome file generato: $filename")

    // 2. Definisci il percorso di storage
    val storagePath = "users/$uid/gallery/$filename"
    Log.d("ImageUpload", "Percorso di storage: $storagePath")

    // 3. Carica l'immagine su Firebase Storage specificando il contentType
    val storageRef = Firebase.storage.reference.child(storagePath)
    val metadata = com.google.firebase.storage.StorageMetadata.Builder()
        .setContentType("image/webp")
        .build()

    storageRef.putFile(uploadUri, metadata)
        .addOnSuccessListener { taskSnapshot ->
            // 4. Recupera l'URL di download dopo il caricamento
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val imageUrl = downloadUri.toString()
                Log.d("ImageUpload", "Immagine caricata con successo. URL: $imageUrl")
                onComplete(imageUrl)  // Notifica il completamento con l'URL
            }.addOnFailureListener { e ->
                Log.e("ImageUpload", "Errore nel recupero dell'URL di download: ${e.message}")
                onComplete("")  // Notifica il fallimento
            }
        }
        .addOnFailureListener { e ->
            Log.e("ImageUpload", "Errore nel caricamento dell'immagine: ${e.message}")
            onComplete("")  // Notifica il fallimento
        }
}

/**
 * Costruisce l'URL di una miniatura ridimensionata dall'URL originale.
 * Si aspetta che le miniature siano in una cartella "gallery_resized"
 * e abbiano un suffisso tipo "_200x200" prima dell'estensione.
 *
 * @param originalUrl L'URL completo dell'immagine originale su Firebase Storage.
 * @param size Il suffisso della dimensione (es. "200x200").
 * @return L'URL della miniatura o null se l'URL originale non è valido o la costruzione fallisce.
 */
fun getThumbnailUrl(originalUrl: String?, size: String = "200x200"): String? {
    if (originalUrl.isNullOrBlank()) return originalUrl
    if (!originalUrl.contains("/gallery") && !originalUrl.contains("%2Fgallery")) return originalUrl

    return try {
        val oIndex = originalUrl.indexOf("/o/")
        if (oIndex == -1) return originalUrl

        val baseUrl = originalUrl.substring(0, oIndex + 3) // include ".../o/"
        val rest = originalUrl.substring(oIndex + 3)

        val queryIndex = rest.indexOf('?')
        val objectPathEncoded = if (queryIndex != -1) rest.substring(0, queryIndex) else rest
        val queryParams = if (queryIndex != -1) rest.substring(queryIndex) else ""

        val objectPathDecoded = URLDecoder.decode(objectPathEncoded, "UTF-8")
        if (!objectPathDecoded.contains("/gallery/")) return originalUrl

        val newObjectPath = objectPathDecoded.replace("/gallery/", "/gallery_resized/")
        val lastDotIndex = newObjectPath.lastIndexOf('.')
        val pathWithSuffix = if (lastDotIndex != -1) {
            "${newObjectPath.substring(0, lastDotIndex)}_${size}${newObjectPath.substring(lastDotIndex)}"
        } else {
            "${newObjectPath}_${size}"
        }

        val reEncodedPath = Uri.encode(pathWithSuffix)
        "$baseUrl$reEncodedPath$queryParams"
    } catch (e: Exception) {
        Log.e("getThumbnailUrl", "Errore costruzione URL thumbnail per: $originalUrl", e)
        originalUrl
    }
}

// TODO: TITOLO AMBIGUO!
suspend fun uploadMultipleImages(
    uid: String,
    uris: List<Uri>,
    context: Context
): List<Pair<String, String?>> {
    val storage = Firebase.storage
    val results = mutableListOf<Pair<String, String?>>()
    val metadata = com.google.firebase.storage.StorageMetadata.Builder()
        .setContentType("image/webp")
        .build()

    uris.forEach { uri ->
        // Calcola l'hash MD5 per l'immagine
        val md5Hash = generateMD5HashFromUri(context, uri)
        val filename = if (md5Hash != null) "$md5Hash.webp" else "${System.currentTimeMillis()}.webp"

        // Usa il filename generato
        val imageRef = storage.reference.child("users/$uid/gallery/$filename")

        val downloadUrl = imageRef.putFile(uri, metadata)
            .await()
            .storage
            .downloadUrl
            .await()
            .toString()

        results.add(Pair(downloadUrl, md5Hash))
    }

    return results
}


/**
* Deletes a profile image from Firebase Storage based on its URL.
*
* @param imageUrl The URL of the image to be deleted.
* @param onComplete Callback to indicate success or failure of deletion. Returns true if successful, false otherwise.
*/
fun deleteProfileImageFromStorage(imageUrl: String, onComplete: (Boolean) -> Unit) {
    Log.d("ImageUploadUtils", "deleteProfileImageFromStorage STARTED - imageUrl: $imageUrl")

    if (imageUrl.isEmpty()) {
        Log.w("ImageUploadUtils", "deleteProfileImageFromStorage: Image URL is empty, cannot delete.")
        onComplete(false)
        Log.d("ImageUploadUtils", "deleteProfileImageFromStorage ENDED - URL vuoto")
        return
    }

    val storage = Firebase.storage
    // Get the StorageReference from the image URL
    val imageRef = storage.getReferenceFromUrl(imageUrl)

    imageRef.delete()
        .addOnSuccessListener {
            Log.d("ImageUploadUtils", "Image successfully deleted from Storage: $imageUrl")
            onComplete(true)
            Log.d("ImageUploadUtils", "deleteProfileImageFromStorage ENDED - SUCCESSO - imageUrl: $imageUrl") // 🚩 LOG FINE FUNZIONE - SUCCESSO
        }
        .addOnFailureListener { e ->
            Log.e("ImageUploadUtils", "Failed to delete image from Storage: $imageUrl, error: ${e.message}")
            onComplete(false)
            Log.d("ImageUploadUtils", "deleteProfileImageFromStorage ENDED - FALLIMENTO - imageUrl: $imageUrl, error: ${e.message}") // 🚩 LOG FINE FUNZIONE - FALLIMENTO
        }

}

/**
 * Genera l'hash MD5 del contenuto di un'immagine specificata tramite Uri.
 *
 * @param context Il contesto Android necessario per accedere al ContentResolver.
 * @param uri L'Uri dell'immagine di cui calcolare l'hash.
 * @return La stringa esadecimale rappresentante l'hash MD5 del contenuto dell'immagine,
 *         oppure null in caso di errore (es. Uri non valido, errore di lettura).
 */
fun generateMD5HashFromUri(context: Context, uri: Uri): String? {
    Log.d("ImageUploadUtils", "generateMD5HashFromUri STARTED - Uri: $uri") // 🚩 LOG INIZIO FUNZIONE
    val contentResolver: ContentResolver = context.contentResolver
    return try {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        inputStream?.use { input ->
            val messageDigest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                messageDigest.update(buffer, 0, bytesRead)
            }
            val digestBytes: ByteArray = messageDigest.digest()
            val hexStringHash = digestBytes.toHexString() // Converti byte array in stringa esadecimale
            Log.d("ImageUploadUtils", "generateMD5HashFromUri SUCCESS - Hash: $hexStringHash")

            hexStringHash // Converti byte array in stringa esadecimale
        }
    } catch (e: Exception) {
        Log.e("ImageUploadUtils", "generateMD5HashFromUri ERROR - Uri: $uri, Error: ${e.message}")
        null // Restituisci null in caso di errore
    } finally {
        Log.d("ImageUploadUtils", "generateMD5HashFromUri ENDED - Uri: $uri")
    }
}

/**
 * Funzione di estensione per convertire un ByteArray in una stringa esadecimale.
 */
fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

/**
 * Verifica che l'URI rappresenti un'immagine con formato consentito.
 * I formati consentiti sono: jpg, jpeg, png, webp.
 */

fun isValidImageUri(context: Context, uri: Uri): Boolean {
    val allowedExtensions = listOf("jpg", "jpeg", "png", "webp")
    var fileName: String? = null

    // Se l'URI usa "content://", estrai il vero nome del file
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) {
                fileName = it.getString(nameIndex)
            }
        }
    } else {
        // Se è un file normale, ottieni il nome dal percorso
        fileName = uri.path?.substringAfterLast("/")
    }

    Log.d("ImageValidation", "File name: $fileName")

    // Estrai l'estensione dal nome file
    val extension = fileName?.substringAfterLast('.', "")?.lowercase()
    Log.d("ImageValidation", "Extracted extension: $extension")

    val isValid = extension in allowedExtensions
    Log.d("ImageValidation", "Is valid: $isValid")

    return isValid
}

/**
 * Converte un'immagine da URI a formato WebP.
 * La funzione restituisce un array di byte rappresentante l'immagine compressa.
 * La qualità (default 80) può essere modificata se necessario.
 */
fun convertImageUriToWebP(context: Context, imageUri: Uri, quality: Int = 80): ByteArray? {
    return try {
        val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        val outputStream = ByteArrayOutputStream()
        // // Uso WEBP per compatibilità con API 24+
        bitmap.compress(Bitmap.CompressFormat.WEBP, quality, outputStream)
        outputStream.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Salva un array di byte in un file temporaneo e restituisce l'URI.
 * Ricorda di configurare un FileProvider nel manifest se non è già fatto.
 */
fun saveByteArrayToTempFile(context: Context, data: ByteArray, filename: String): Uri? {
    return try {
        val tempFile = File(context.cacheDir, filename)
        tempFile.writeBytes(data)
        // Assicurati di avere configurato il FileProvider nel manifest
        FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Rate limiter per gli upload di immagini.
 * Consente al massimo 10 upload ogni 60 secondi per utente.
 */
object UploadRateLimiter {
    private val userUploadTimestamps = ConcurrentHashMap<String, MutableList<Long>>()
    private const val TIME_WINDOW = 40 * 1000L  // 40 secondi
    private const val MAX_UPLOADS = 4          // massimo 4 upload per finestra

    fun canUpload(userId: String): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = userUploadTimestamps.getOrPut(userId) { mutableListOf() }
        // Rimuove i timestamp più vecchi della finestra
        timestamps.removeAll { now - it > TIME_WINDOW }
        return if (timestamps.size < MAX_UPLOADS) {
            timestamps.add(now)
            true
        } else {
            false
        }
    }

}
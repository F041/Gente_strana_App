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

    // 1. Genera l'hash MD5 dall'URI dell'immagine
    val md5Hash = generateMD5HashFromUri(context, imageUri)
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

    // 3. Carica l'immagine su Firebase Storage
    val storageRef = Firebase.storage.reference.child(storagePath)
    storageRef.putFile(imageUri)
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
 * Si aspetta che le miniature siano in una cartella "_resized"
 * e abbiano un suffisso tipo "_200x200" prima dell'estensione.
 *
 * @param originalUrl L'URL completo dell'immagine originale su Firebase Storage.
 * @param size Il suffisso della dimensione (es. "200x200").
 * @return L'URL della miniatura o null se l'URL originale non è valido o la costruzione fallisce.
 */
fun getThumbnailUrl(originalUrl: String?, size: String = "200x200"): String? {
    if (originalUrl.isNullOrBlank() || !originalUrl.contains("/users%2F")) {
        // Se l'URL è nullo, vuoto o non sembra un URL di storage valido per le gallerie utente
        return originalUrl // Restituisci l'originale (o null se era null)
    }

    return try {
        // 1. Decodifica l'URL per lavorare con i path normali
        val decodedUrl = URLDecoder.decode(originalUrl, "UTF-8")

        // 2. Trova l'ultima occorrenza di "/" per separare percorso e nome file
        val lastSlashIndex = decodedUrl.lastIndexOf('/')
        if (lastSlashIndex == -1 || lastSlashIndex == decodedUrl.length - 1) {
            return originalUrl // Non c'è nome file? Restituisci originale
        }
        val filePath = decodedUrl.substring(0, lastSlashIndex)
        val fullFileName = decodedUrl.substring(lastSlashIndex + 1)

        // 3. Trova l'ultima occorrenza di "." per separare nome e estensione
        val lastDotIndex = fullFileName.lastIndexOf('.')
        val fileName = if (lastDotIndex != -1) fullFileName.substring(0, lastDotIndex) else fullFileName
        val extension = if (lastDotIndex != -1) fullFileName.substring(lastDotIndex) else "" // include il "."

        // 4. Modifica il percorso inserendo "_resized" prima del nome file
        // Assumiamo che il percorso originale sia tipo ".../gallery/nomefile.ext"
        // e vogliamo ".../gallery_resized/nomefile_sizexsize.ext"
        val resizedFilePath = filePath.replace("/gallery", "/gallery_resized", ignoreCase = true)

        // 5. Costruisci il nuovo nome file con il suffisso della dimensione
        val resizedFileName = "${fileName}_${size}${extension}"

        // 6. Ricodifica le parti del percorso e nome file separatamente per sicurezza
        //    (Anche se gli hash MD5 non dovrebbero contenere caratteri speciali, è più robusto)
        //    Prima dividiamo il percorso in segmenti
        val pathSegments = resizedFilePath.split('/')
        val encodedPathSegments = pathSegments.map { URLEncoder.encode(it, "UTF-8") }
        //    Ricostruiamo il percorso codificato, gestendo gli slash iniziali/doppi
        val encodedResizedPath = encodedPathSegments.joinToString("/")
            .replace("%2F", "/") // Assicura che gli slash rimangano slash
            .replace("//", "/") // Evita slash doppi

        val encodedResizedFileName = URLEncoder.encode(resizedFileName, "UTF-8")

        // 7. Ricostruisci l'URL completo, trovando la parte base prima di "/users%2F"
        val baseUrlEndIndex = originalUrl.indexOf("/users%2F")
        if (baseUrlEndIndex == -1) return originalUrl // Se non troviamo "/users%2F", qualcosa è strano

        val baseUrl = originalUrl.substring(0, baseUrlEndIndex)

        // 8. Combina base URL, percorso codificato e nome file codificato
        //    Assicurati che ci sia uno e un solo slash tra base e percorso
        val finalThumbnailUrl = "${baseUrl.removeSuffix("/")}/${encodedResizedPath.removePrefix("/")}/${encodedResizedFileName}"

        // 9. Recupera eventuali token dall'URL originale
        val tokenPart = originalUrl.substringAfter("?", "")
        if (tokenPart.isNotEmpty()) {
            "$finalThumbnailUrl?$tokenPart"
        } else {
            finalThumbnailUrl
        }

    } catch (e: Exception) {
        Log.e("getThumbnailUrl", "Errore costruzione URL thumbnail per: $originalUrl", e)
        originalUrl // In caso di errore, restituisci l'URL originale
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

    uris.forEach { uri ->
        // Calcola l'hash MD5 per l'immagine
        val md5Hash = generateMD5HashFromUri(context, uri)
        val filename = if (md5Hash != null) "$md5Hash.webp" else "${System.currentTimeMillis()}.webp"

        // Usa il filename generato
        val imageRef = storage.reference.child("users/$uid/gallery/$filename")

        val downloadUrl = imageRef.putFile(uri)
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
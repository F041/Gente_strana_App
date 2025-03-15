package com.gentestrana.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.security.MessageDigest


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

    // 1. Genera l'hash MD5 dall'URI dell'immagine
    val md5Hash = generateMD5HashFromUri(context, imageUri)
    val filename = if (md5Hash != null) {
        "$md5Hash.jpg"  // Usa l'hash MD5 come nome del file
    } else {
        // Fallback: usa un timestamp se l'hash non è disponibile
        Log.w("ImageUpload", "MD5 hash non disponibile, uso timestamp come fallback")
        "${System.currentTimeMillis()}.jpg"
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
        val filename = if (md5Hash != null) "$md5Hash.jpg" else "${System.currentTimeMillis()}.jpg"

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
            Log.d("ImageUploadUtils", "generateMD5HashFromUri SUCCESS - Hash: $hexStringHash") // 🚩 LOG SUCCESSO
            hexStringHash // Converti byte array in stringa esadecimale
        }
    } catch (e: Exception) {
        Log.e("ImageUploadUtils", "generateMD5HashFromUri ERROR - Uri: $uri, Error: ${e.message}") // 🚩 LOG ERRORE
        null // Restituisci null in caso di errore
    } finally {
        Log.d("ImageUploadUtils", "generateMD5HashFromUri ENDED - Uri: $uri") // 🚩 LOG FINE FUNZIONE
    }
}

/**
 * Funzione di estensione per convertire un ByteArray in una stringa esadecimale.
 */
fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
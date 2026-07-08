package com.gentestrana

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Test per VERIFICARE che i fix ai bug di caricamento immagini siano stati applicati
 * e che il codice ora sia corretto.
 *
 * Bug originali e fix applicati:
 *
 * BUG 1 - UserItem.kt (codice morto, non fixato)
 * BUG 2 - ServiceProfileCard.kt: fallback "res/drawable/random_user.webp" -> R.drawable.random_user
 * BUG 3 - ReorderableProfileImageGridWithAdd.kt: index misalignment -> imageList.indexOf(item)
 * BUG 4 - ProfileViewModel.kt: race condition -> usa risultato dalla transazione Firestore
 */
class ImageLoadingCrashTest {

    private val rootDir: String
        get() {
            // Prova vari path per trovare la root del progetto
            val candidates = listOf(
                ".",                          // Eseguito da app/
                "..",                         // Eseguito da progetto/
                "../..",                      // Eseguito da app/build/
                System.getProperty("user.dir") ?: "."
            )
            for (candidate in candidates) {
                val dir = File(candidate)
                if (File(dir, "settings.gradle.kts").exists()) return dir.absolutePath
                if (File(dir, "app").exists() && File(dir, "build.gradle.kts").exists()) return dir.absolutePath
            }
            return candidates.last()
        }

    @Test
    fun verify_fix_BUG2_ServiceProfileCard_fallback_is_valid_R_drawable() {
        val file = File("$rootDir/app/src/main/java/com/gentestrana/users/ServiceProfileCard.kt")
        assertTrue("ServiceProfileCard.kt non trovato in: ${file.absolutePath}", file.exists())

        val content = file.readText()

        // Il vecchio fallback "res/drawable/random_user.webp" NON deve più esserci
        assertFalse("BUG 2: rimosso 'res/drawable/random_user.webp'",
            content.contains("res/drawable/random_user.webp"))

        // R.drawable.random_user deve essere usato come fallback
        assertTrue("BUG 2: R.drawable.random_user presente come fallback",
            content.contains("R.drawable.random_user"))

        // ImageRequest.Builder deve essere usato
        assertTrue("BUG 2: ImageRequest.Builder presente",
            content.contains("ImageRequest.Builder"))
    }

    @Test
    fun verify_fix_BUG3_delete_uses_indexOf_not_raw_index() {
        val file = File("$rootDir/app/src/main/java/com/gentestrana/components/ReorderableProfileImageGallery.kt")
        assertTrue("ReorderableProfileImageGallery.kt non trovato in: ${file.absolutePath}", file.exists())

        val content = file.readText()

        // imageList.indexOf(item) deve essere usato nel delete handler
        assertTrue("BUG 3: delete handler usa imageList.indexOf(item)",
            content.contains("imageList.indexOf(item)"))

        // NON deve più usare imageList.removeAt(index) (con l'indice raw della displayList)
        assertFalse("BUG 3: rimosso imageList.removeAt(index) non protetto",
            content.contains("imageList.removeAt(index)"))
    }

    @Test
    fun verify_fix_BUG4_upload_uses_transaction_result_not_stale_local_state() {
        val file = File("$rootDir/app/src/main/java/com/gentestrana/ui_controller/ProfileViewModel.kt")
        assertTrue("ProfileViewModel.kt non trovato in: ${file.absolutePath}", file.exists())

        val content = file.readText()

        // _profilePicUrl.value deve usare updatedUrlsFromTransaction (risultato transazione)
        assertTrue("BUG 4: _profilePicUrl.value = updatedUrlsFromTransaction presente",
            content.contains("_profilePicUrl.value = updatedUrlsFromTransaction"))

        // NON deve più usare _profilePicUrl.value = _profilePicUrl.value + newUrl
        val wrongPattern = Regex("""_profilePicUrl\.value\s*=\s*_profilePicUrl\.value\s*\+\s*newUrl""")
        assertFalse("BUG 4: rimosso _profilePicUrl.value = _profilePicUrl.value + newUrl",
            wrongPattern.containsMatchIn(content))
    }
}

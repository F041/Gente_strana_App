package com.gentestrana.utils

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel








object TranslationHelper {

    /**
     * Rileva la lingua del testo fornito.
     *
     * @param text Il testo di cui identificare la lingua.
     * @param onSuccess Callback che restituisce il codice ISO della lingua identificata (es. "fr", "it").
     * @param onFailure Callback in caso di errore.
     */
    private fun detectLanguage(
        text: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    onFailure(Exception("Lingua non identificata"))
                } else {
                    onSuccess(languageCode)
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Traduce il testo fornito in base alla lingua rilevata come source e al codice della lingua target.
     *
     * @param text Il testo da tradurre.
     * @param targetLanguageCode Il codice della lingua target (es. "it" per italiano).
     * @param onSuccess Callback con il testo tradotto.
     * @param onFailure Callback in caso di errore.
     */
    fun translateTextWithDetection(
        text: String,
        targetLanguageCode: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onDownloadStarted: () -> Unit,
        onDownloadCompleted: () -> Unit
    ) {
        println("🟢 Avvio rilevamento lingua per: $text")

        detectLanguage(text, { sourceLanguage ->
            println("🟠 Lingua rilevata: $sourceLanguage, target: $targetLanguageCode")

            if (sourceLanguage.equals(targetLanguageCode, ignoreCase = true)) {
                println("✅ La lingua sorgente è uguale alla lingua target, nessuna traduzione necessaria")
                onSuccess(text)
                return@detectLanguage
            }

            val sourceLangMLKit = TranslateLanguage.fromLanguageTag(sourceLanguage) ?: TranslateLanguage.ENGLISH
            val targetLangMLKit = TranslateLanguage.fromLanguageTag(targetLanguageCode) ?: TranslateLanguage.ENGLISH

            println("🔵 ML Kit - Source: $sourceLangMLKit, Target: $targetLangMLKit")

            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLangMLKit)
                .setTargetLanguage(targetLangMLKit)
                .build()
            val translator = Translation.getClient(options)

            val conditions = DownloadConditions.Builder().requireWifi().build()
            isTranslationModelDownloaded(targetLanguageCode) { isDownloaded ->
                if (!isDownloaded) {
                    println("⏳ Modello non presente, avvio download...")
                    onDownloadStarted() // Notifica l'inizio del download
                } else {
                    println("✅ Modello già presente.")
                }

                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        println("✅ Download (se necessario) completato.")
                        if (!isDownloaded) {
                            onDownloadCompleted() // Notifica la fine del download SOLO se è avvenuto
                        }
                        println("Avvio traduzione...")
                        translator.translate(text)
                            .addOnSuccessListener { translatedText ->
                                println("✅ Traduzione completata: $translatedText")
                                onSuccess(translatedText)
                            }
                            .addOnFailureListener { exception ->
                                println("❌ Errore nella traduzione: ${exception.message}")
                                if (!isDownloaded) onDownloadCompleted() // Assicurati di notificare anche in caso di fallimento post-download
                                onFailure(exception)
                            }.addOnCompleteListener {
                                // Chiudi il translator per liberare risorse quando finito
                                translator.close()
                            }
                    }
                    .addOnFailureListener { exception ->
                        println("❌ Errore nel download del modello: ${exception.message}")
                        if (!isDownloaded) {
                            onDownloadCompleted() // Notifica la fine del (tentativo di) download
                        }
                        onFailure(exception)
                        translator.close() // Chiudi anche in caso di fallimento download
                    }
            } // Fine callback isTranslationModelDownloaded

        }, { exception ->
            println("❌ Errore nel rilevamento lingua: ${exception.message}")
            onFailure(exception)
        })
    }

    /**
     * Controlla se un modello di traduzione è già scaricato.
     *
     * @param languageCode Codice della lingua da verificare.
     * @param onResult Callback che restituisce `true` se il modello è presente, `false` altrimenti.
     */
    fun isTranslationModelDownloaded(
        languageCode: String,
        onResult: (Boolean) -> Unit
    ) {
        // 1. Crea il modello correttamente
        val model = TranslateRemoteModel.Builder(languageCode).build()

        // 2. Ottieni il model manager corretto
        val modelManager = RemoteModelManager.getInstance()
        // preso da documentazione ufficiale, né chatGPT né Qwen hanno saputo risolvere

        // 3. Specifica il tipo generico esplicitamente
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models: Set<TranslateRemoteModel> ->
                onResult(models.contains(model))
            }
            .addOnFailureListener {
                onResult(false)
            }
    }
}




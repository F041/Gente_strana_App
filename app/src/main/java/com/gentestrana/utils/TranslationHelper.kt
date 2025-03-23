package com.gentestrana.utils

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions


object TranslationHelper {
    /**
     * Traduce il testo fornito in base al codice della lingua target.
     *
     * @param text Il testo da tradurre.
     * @param targetLanguageCode Il codice della lingua target (es. "it" per italiano).
     * @param onSuccess Callback con il testo tradotto.
     * @param onFailure Callback in caso di errore.
     */
    fun translateText(
        text: String,
        targetLanguageCode: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Configuriamo il translator: presupponiamo che la lingua sorgente sia l'inglese (default)
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(
                // Traduci il codice target in una costante ML Kit; se non riconosciuto, torna all'inglese
                TranslateLanguage.fromLanguageTag(targetLanguageCode) ?: TranslateLanguage.ENGLISH
            )
            .build()
        val translator = Translation.getClient(options)

        // Condizioni per scaricare il modello (es. solo con WiFi)
        val conditions = DownloadConditions.Builder().requireWifi().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        onSuccess(translatedText)
                    }
                    .addOnFailureListener { exception ->
                        onFailure(exception)
                    }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}

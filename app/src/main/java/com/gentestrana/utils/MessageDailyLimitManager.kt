package com.gentestrana.utils

import android.content.Context
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig

object MessageDailyLimitManager {
    private const val PREFS_NAME = "message_limit_prefs"
    private const val KEY_COUNT = "daily_message_count"
    private const val KEY_TIMESTAMP = "daily_message_timestamp"
    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    // Chiave per Remote Config (meglio definire come costante)
    private const val REMOTE_CONFIG_DAILY_LIMIT_KEY = "daily_message_limit"
    // Valore di default DA USARE nel codice se Remote Config fallisce
    private const val DEFAULT_DAILY_LIMIT = 300L // Definiamo il default qui come Long

    /**
     * Verifica se l'utente (identificato da userId) può ancora inviare messaggi oggi.
     * Legge il limite da Remote Config.
     */
    fun canSendMessage(context: Context, userId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastReset = prefs.getLong("$userId-$KEY_TIMESTAMP", 0L)
        val currentTime = System.currentTimeMillis()
        var count = prefs.getInt("$userId-$KEY_COUNT", 0)

        // Se l'ultimo reset è più vecchio di 24 ore, resetta il conteggio
        if (currentTime - lastReset > DAY_MILLIS) {
            count = 0
            prefs.edit().putInt("$userId-$KEY_COUNT", count)
                .putLong("$userId-$KEY_TIMESTAMP", currentTime)
                .apply()
            Log.d("DailyLimit", "Daily count reset for user $userId") // Log per debug
        }

        // Leggi il limite da Remote Config ---
        val remoteConfig = Firebase.remoteConfig
        // getLong restituisce il valore dal cloud se disponibile e attivato,
        // altrimenti restituisce il valore di default impostato in setDefaultsAsync,
        // o 0L se non è stato impostato nessun default.
        // Usiamo il nostro DEFAULT_DAILY_LIMIT come fallback finale sicuro.
        val currentLimit = remoteConfig.getLong(REMOTE_CONFIG_DAILY_LIMIT_KEY)
        // Se getLong restituisce 0L (perché magari il fetch è fallito e non c'era default)
        // usiamo il nostro DEFAULT_DAILY_LIMIT definito nell'oggetto.
        val actualLimit = if (currentLimit > 0) currentLimit else DEFAULT_DAILY_LIMIT
        Log.d("DailyLimit", "User $userId: Count=$count, Limit=$actualLimit") // Log per debug

        return count < actualLimit // Usa il limite letto da Remote Config (o il default)
    }

    /**
     * Incrementa il conteggio dei messaggi inviati per l'utente.
     * (Questa funzione rimane invariata)
     */
    fun incrementCount(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("$userId-$KEY_COUNT", 0)
        prefs.edit().putInt("$userId-$KEY_COUNT", currentCount + 1).apply()
    }
}
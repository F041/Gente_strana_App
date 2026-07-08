package com.gentestrana.utils

import android.content.Context
import com.gentestrana.R

/**
 * Utility per la gestione degli icebreaker (domande rompighiaccio) in chat.
 *
 * Fase 0 del piano Icebreaker → Autopilot.
 * Carica le domande dalle string resource (arrays.xml) così da supportare
 * EN (values/arrays.xml) e IT (values-it/arrays.xml) automaticamente.
 * Le domande vengono mostrate solo quando la chat è vuota (nessun messaggio scambiato).
 */
object IcebreakerUtils {

    /**
     * Seleziona [count] domande casuali dalla resource [R.array.icebreakers].
     * Il caricamento dalle resource garantisce che la lingua corretta venga usata
     * in base alle impostazioni del dispositivo.
     *
     * @param context Serve per accedere a [context.resources]
     * @param count Quante domande selezionare (default 3)
     * @return Lista di [count] domande casuali, o lista vuota se l'array è vuoto
     */
    fun getRandomIcebreakers(context: Context, count: Int = 3): List<String> {
        val allIcebreakers = context.resources.getStringArray(R.array.icebreakers).toList()
        if (allIcebreakers.isEmpty()) return emptyList()
        val shuffled = allIcebreakers.shuffled()
        return shuffled.take(minOf(count, shuffled.size))
    }
}
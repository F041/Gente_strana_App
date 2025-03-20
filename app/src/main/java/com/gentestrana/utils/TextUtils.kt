package com.gentestrana.utils

/**
 * Rimuove tutti gli spazi bianchi (inclusi tab, nuove righe, ecc.) dalla stringa.
 */

fun removeSpaces(input: String): String {
    return input.trim().replace(Regex("\\s+"), " ")
    // Sostituisce spazi multipli con uno solo
}

/**
 * Sanitizza l'input rimuovendo tag HTML e altri caratteri potenzialmente pericolosi.
 * Questa è una versione semplificata; in un'app reale potresti utilizzare una libreria dedicata.
 */
fun sanitizeInput(input: String): String {
    // Rimuove eventuali tag HTML (semplificato)
    return input.replace(Regex("<[^>]*>"), "")
}

/**
 * Normalizza un URL rimuovendo la parte relativa al token e parametri di cache.
 * Questa normalizzazione semplifica il confronto di URL per verificare duplicati.
 */
fun String.normalizeUrl(): String {
    return this.substringBefore("?") // Rimuove tutto dopo il primo '?'
}
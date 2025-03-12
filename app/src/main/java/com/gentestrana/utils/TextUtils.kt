package com.gentestrana.utils

/**
 * Rimuove tutti gli spazi bianchi (inclusi tab, nuove righe, ecc.) dalla stringa.
 */

fun removeSpaces(text: String): String {
    return text.replace("\\s+".toRegex(), "")
}

/**
 * Normalizza un URL rimuovendo la parte relativa al token e parametri di cache.
 * Questa normalizzazione semplifica il confronto di URL per verificare duplicati.
 */
fun String.normalizeUrl(): String {
    return this.substringBefore("?") // Rimuove tutto dopo il primo '?'
}
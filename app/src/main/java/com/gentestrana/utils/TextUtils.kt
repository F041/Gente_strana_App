package com.gentestrana.utils

/**
 * Rimuove tutti gli spazi bianchi (inclusi tab, nuove righe, ecc.) dalla stringa.
 */

fun removeSpaces(text: String): String {
    return text.replace("\\s+".toRegex(), "")
}

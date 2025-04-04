package com.gentestrana.utils

import com.gentestrana.users.User
import java.util.Locale
import kotlin.math.sqrt

// Funzione per pulire il testo: minuscolo e rimuove punteggiatura
fun preprocessText(text: String): List<String> {
    // TODO: mancano stopwords
    return text
        .lowercase(Locale.getDefault())
        .replace(Regex("[^a-z0-9\\s]"), "")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
}

// Calcola il vettore TF come mappa parola -> frequenza
fun computeTermFrequency(tokens: List<String>): Map<String, Int> {
    val tf = mutableMapOf<String, Int>()
    tokens.forEach { token ->
        tf[token] = tf.getOrDefault(token, 0) + 1
    }
    return tf
}

// Calcola il prodotto scalare tra due vettori (rappresentati come mappe)
fun dotProduct(vec1: Map<String, Int>, vec2: Map<String, Int>): Int {
    return vec1.entries.sumOf { (key, value) -> value * (vec2[key] ?: 0) }
}

// Calcola la norma di un vettore
fun vectorNorm(vec: Map<String, Int>): Double {
    return sqrt(vec.values.sumOf { it * it }.toDouble())
}

// Calcola la similarità coseno tra due testi
fun cosineSimilarity(text1: String, text2: String): Double {
    val tokens1 = preprocessText(text1)
    val tokens2 = preprocessText(text2)

    val tf1 = computeTermFrequency(tokens1)
    val tf2 = computeTermFrequency(tokens2)

    val dot = dotProduct(tf1, tf2)
    val norm1 = vectorNorm(tf1)
    val norm2 = vectorNorm(tf2)

    return if (norm1 != 0.0 && norm2 != 0.0) dot / (norm1 * norm2) else 0.0
}

fun getUserTextProfile(user: User): String {
    // Combina bio e topics in un'unica stringa
    val bio = user.bio
    val topics = user.topics.joinToString(" ")
    return "$bio $topics"
}

fun computeUserSimilarity(user1: User, user2: User): Double {
    // TODO: soffre la differenza di lingua!
    val profileText1 = getUserTextProfile(user1)
    val profileText2 = getUserTextProfile(user2)
    return cosineSimilarity(profileText1, profileText2)
}


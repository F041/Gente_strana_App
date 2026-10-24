package com.gentestrana.utils

import android.content.Context
import com.gentestrana.R

/**
 * Utility per la gestione degli icebreaker (domande rompighiaccio) in chat.
 *
 * Fase 0 + Fase 1 del piano Icebreaker → Autopilot.
 * Genera domande rompighiaccio per ogni chat vuota basandosi sugli argomenti (topics) e la bio del destinatario.
 */
object IcebreakerUtils {

    private val categoryKeywords: Map<String, List<String>> = mapOf(
        "Viaggi" to listOf("viaggio", "viaggiare", "vacanza", "volare", "turista", "zaino", "travel", "trip", "holiday"),
        "Fotografia" to listOf("foto", "fotografia", "scattare", "obiettivo", "reflex", "photography", "photo", "camera"),
        "Lettura" to listOf("libro", "leggere", "lettura", "romanzo", "biblioteca", "book", "reading", "novel"),
        "Natura" to listOf("montagna", "natura", "trekking", "escursione", "bosco", "mare", "spiaggia", "nature", "mountain", "hiking", "beach"),
        "Cucina" to listOf("cucina", "cucinare", "ricetta", "cibo", "chef", "cooking", "food", "recipe", "baking"),
        "Cinema" to listOf("film", "cinema", "serie", "regista", "netflix", "movie", "tv series", "director"),
        "Musica" to listOf("musica", "cantare", "concerto", "chitarra", "band", "music", "singing", "concert", "guitar"),
        "Sport" to listOf("sport", "corsa", "palestra", "calcio", "nuoto", "sports", "running", "gym", "football", "swimming"),
        "Arte" to listOf("arte", "disegno", "pittura", "museo", "creativo", "art", "drawing", "painting", "museum", "creative"),
        "Animali" to listOf("animali", "cane", "gatto", "pet", "cucciolo", "animals", "dog", "cat", "pets"),
        "Gaming" to listOf("gioco", "videogioco", "gaming", "rpg", "multiplayer", "game", "videogame", "gaming"),
        "Tecnologia" to listOf("tech", "programmazione", "codice", "app", "startup", "technology", "programming", "code", "software")
    )

    private val categoryQuestions: Map<String, List<String>> = mapOf(
        "Viaggi" to listOf(
            "Qual è stato il tuo viaggio più memorabile?",
            "Se potessi partire domani, dove andresti?",
            "Preferisci viaggiare da solo o in compagnia?",
            "What has been your most memorable trip?",
            "If you could leave tomorrow, where would you go?",
            "Do you prefer traveling alone or with company?"
        ),
        "Fotografia" to listOf(
            "Da quanto tempo ti dedichi alla fotografia?",
            "Cosa preferisci fotografare?",
            "How long have you been into photography?",
            "What do you prefer to photograph?"
        ),
        "Lettura" to listOf(
            "Qual è l'ultimo libro che hai letto?",
            "Che genere di libri preferisci?",
            "What is the last book you read?",
            "What genre of books do you prefer?"
        ),
        "Natura" to listOf(
            "La montagna o il mare? E qual è il tuo posto preferito?",
            "Ti piace fare escursioni all'aria aperta?",
            "Mountains or sea? And what is your favorite spot?",
            "Do you enjoy outdoor hiking?"
        ),
        "Cucina" to listOf(
            "Sai cucinare o sei più da takeaway? 🍝",
            "Qual è il tuo piatto preferito?",
            "Can you cook or are you more of a takeaway person? 🍝",
            "What is your favorite dish?"
        ),
        "Cinema" to listOf(
            "Hai visto qualcosa di bello ultimamente?",
            "Che genere di film preferisci?",
            "Have you watched anything good lately?",
            "What movie genre do you prefer?"
        ),
        "Musica" to listOf(
            "Che genere musicale ascolti di più?",
            "Suoni qualche strumento?",
            "What music genre do you listen to the most?",
            "Do you play any instrument?"
        ),
        "Sport" to listOf(
            "Fai sport? Da quanto tempo?",
            "Qual è lo sport che preferisci?",
            "Do you play sports? For how long?",
            "What is your favorite sport?"
        ),
        "Arte" to listOf(
            "Ti piace l'arte? Hai un artista preferito?",
            "Disegni o dipingi?",
            "Do you like art? Do you have a favorite artist?",
            "Do you draw or paint?"
        ),
        "Animali" to listOf(
            "Hai animali domestici?",
            "Qual è il tuo animale preferito?",
            "Do you have any pets?",
            "What is your favorite animal?"
        ),
        "Gaming" to listOf(
            "Giochi ai videogiochi? Quale consiglieresti?",
            "Qual è il tuo genere di gioco preferito?",
            "Do you play video games? Which one would you recommend?",
            "What is your favorite game genre?"
        ),
        "Tecnologia" to listOf(
            "Lavori nel tech o è solo una passione?",
            "Che linguaggio di programmazione preferisci?",
            "Do you work in tech or is it just a passion?",
            "What programming language do you prefer?"
        )
    )

    fun getRandomIcebreakers(context: Context, count: Int = 3): List<String> {
        val allIcebreakers = context.resources.getStringArray(R.array.icebreakers).toList()
        if (allIcebreakers.isEmpty()) return emptyList()
        val shuffled = allIcebreakers.shuffled()
        return shuffled.take(minOf(count, shuffled.size))
    }

    private fun filterQuestionsByLocale(questions: List<String>, context: Context): List<String> {
        if (questions.isEmpty()) return questions
        val appLanguage = context.resources.configuration.locales.get(0).language
        val isItalian = appLanguage == "it"
        val half = questions.size / 2
        return if (isItalian) {
            questions.take(half)
        } else {
            questions.drop(half)
        }
    }

    private fun truncateTopic(topic: String): String {
        val trimmed = topic.trim()
        if (trimmed.length <= 40) return trimmed
        val sub = trimmed.substring(0, 40)
        val lastSpace = sub.lastIndexOf(' ')
        val truncated = if (lastSpace > 10) sub.substring(0, lastSpace) else sub
        return "$truncated…"
    }

    fun matchIcebreakers(
        bio: String?,
        topics: List<String>?,
        context: Context,
        count: Int = 3
    ): List<String> {
        val bioText = bio?.trim()?.lowercase() ?: ""
        val cleanTopics = topics?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

        val templates = try {
            context.resources.getStringArray(R.array.icebreaker_topic_templates).toList()
        } catch (_: Exception) {
            emptyList()
        }

        val topicQuestions = mutableListOf<String>()
        val shuffledTemplates = templates.shuffled()
        var templateIndex = 0

        for (topic in cleanTopics.take(2)) {
            if (shuffledTemplates.isNotEmpty()) {
                val template = shuffledTemplates[templateIndex % shuffledTemplates.size]
                templateIndex++
                val truncated = truncateTopic(topic)
                topicQuestions.add(String.format(template, truncated))
            }
        }

        val matchedCategories = mutableSetOf<String>()
        val textToMatch = "$bioText ${cleanTopics.joinToString(" ").lowercase()}"

        for ((category, keywords) in categoryKeywords) {
            for (keyword in keywords) {
                val regex = Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(textToMatch)) {
                    matchedCategories.add(category)
                    break
                }
            }
        }

        val allCategoryQuestions = matchedCategories.flatMap { category ->
            categoryQuestions[category] ?: emptyList()
        }
        val categoryQuestionsFiltered = filterQuestionsByLocale(allCategoryQuestions, context).shuffled()

        val combined = mutableListOf<String>()
        val maxLen = maxOf(topicQuestions.size, categoryQuestionsFiltered.size)
        for (i in 0 until maxLen) {
            if (i < topicQuestions.size) combined.add(topicQuestions[i])
            if (i < categoryQuestionsFiltered.size) combined.add(categoryQuestionsFiltered[i])
        }

        val result = combined.distinct().take(count)
        if (result.size < count) {
            val genericNeeded = count - result.size
            val genericList = getRandomIcebreakers(context, count = genericNeeded * 2).filterNot { result.contains(it) }
            return (result + genericList).take(count)
        }

        return result
    }
}

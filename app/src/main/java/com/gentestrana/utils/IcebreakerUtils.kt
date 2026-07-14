package com.gentestrana.utils

import android.content.Context
import com.gentestrana.R

/**
 * Utility per la gestione degli icebreaker (domande rompighiaccio) in chat.
 *
 * Fase 0 + Fase 1 del piano Icebreaker → Autopilot.
 * Fase 0: domande casuali da arrays.xml.
 * Fase 1: Smart Matching basato su keyword da bio/topics del destinatario.
 */
object IcebreakerUtils {

    // --- FASE 1: Keyword → Domande mirate ---

    // Mappa: nome categoria → lista di keyword da cercare in bio/topics
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

    // Mappa: nome categoria → domande mirate (localizzate in italiano e inglese)
    // NOTA: in futuro queste potrebbero essere spostate in arrays.xml per full i18n
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

    /**
     * Seleziona [count] domande casuali dalla resource [R.array.icebreakers].
     * (Fase 0 — fallback generico)
     */
    fun getRandomIcebreakers(context: Context, count: Int = 3): List<String> {
        val allIcebreakers = context.resources.getStringArray(R.array.icebreakers).toList()
        if (allIcebreakers.isEmpty()) return emptyList()
        val shuffled = allIcebreakers.shuffled()
        return shuffled.take(minOf(count, shuffled.size))
    }

    /**
     * Seleziona solo le domande nella lingua corrente dell'app (dalle risorse,
     * non dal locale di sistema) da una lista bilingue (IT/EN).
     * Le domande sono organizzate con IT prima, EN dopo in ogni lista categoria.
     */
    private fun filterQuestionsByLocale(questions: List<String>, context: Context): List<String> {
        if (questions.isEmpty()) return questions
        // Usa la lingua delle risorse attive, non il Locale di sistema
        val appLanguage = context.resources.configuration.locales.get(0).language
        val isItalian = appLanguage == "it"
        val half = questions.size / 2
        return if (isItalian) {
            questions.take(half) // Prime metà = italiano
        } else {
            questions.drop(half) // Seconda metà = inglese
        }
    }

    /**
     * Fase 1 — Smart Matching: seleziona domande in base a bio e topics del destinatario.
     *
     * @param bio La bio del destinatario (può essere null o vuota)
     * @param topics La lista di topics del destinatario (può essere vuota)
     * @param context Serve per accedere alle risorse (fallback Fase 0)
     * @param count Quante domande selezionare (default 3)
     * @return Lista di domande mirate nella lingua del dispositivo (se match trovate), altrimenti Fase 0
     */
    fun matchIcebreakers(
        bio: String?,
        topics: List<String>?,
        context: Context,
        count: Int = 3
    ): List<String> {
        val textToMatch = buildString {
            bio?.let { append(" $it") }
            topics?.forEach { append(" $it") }
        }.trim().lowercase()

        // Se non c'è testo da matchare, ricadi su Fase 0
        if (textToMatch.isEmpty()) {
            return getRandomIcebreakers(context, count)
        }

        // Trova le categorie che matchano
        val matchedCategories = mutableSetOf<String>()
        for ((category, keywords) in categoryKeywords) {
            for (keyword in keywords) {
                if (textToMatch.contains(keyword)) {
                    matchedCategories.add(category)
                    break // Una keyword matchata è sufficiente per la categoria
                }
            }
        }

        // Raccogli TUTTE le domande bilingue delle categorie matchate
        val allMatchedQuestions = matchedCategories.flatMap { category ->
            categoryQuestions[category] ?: emptyList()
        }

        // Filtra SOLO quelle nella lingua corrente
        val matchedQuestions = filterQuestionsByLocale(allMatchedQuestions, context)

        // Se abbiamo abbastanza domande nella lingua giusta, pesca count casuali
        if (matchedQuestions.size >= count) {
            return matchedQuestions.shuffled().take(count)
        }

        // Se abbiamo qualche domanda ma non abbastanza, aggiungi dalla Fase 0
        if (matchedQuestions.isNotEmpty()) {
            val randomOnes = getRandomIcebreakers(context, count - matchedQuestions.size)
            return (matchedQuestions.shuffled() + randomOnes).take(count)
        }

        // Nessun match: ricadi su Fase 0
        return getRandomIcebreakers(context, count)
    }
}
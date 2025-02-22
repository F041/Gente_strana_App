package com.gentestrana.utils

import android.content.Context

fun getFlagEmoji(context: Context, code: String): String {
    val supportedLanguageCodes = context.resources.getStringArray(com.gentestrana.R.array.supported_language_codes).toList()
    return if (supportedLanguageCodes.contains(code.lowercase())) {
        when (code.lowercase()) {
            "en" -> "🇬🇧"  // o 🇺🇸 per USA? Considera quale bandiera vuoi mostrare per "en"
            "zh" -> "🇨🇳"
            "hi" -> "🇮🇳"
            "es" -> "🇪🇸"
            "fr" -> "🇫🇷"
            "ar" -> "🇦🇪" // o 🇸🇦 ?  Arabo è parlato in molti paesi, scegli una bandiera rappresentativa o "🌍"
            "bn" -> "🇧🇩"
            "ru" -> "🇷🇺"
            "pt" -> "🇵🇹" // o 🇧🇷 per Brasiliano?
            "id" -> "🇮🇩"
            "de" -> "🇩🇪"
            "ja" -> "🇯🇵"
            "sw" -> "🇹🇿" // o 🇰🇪 ? Swahili è parlato in più paesi
            "ko" -> "🇰🇷"
            "it" -> "🇮🇹"
            "tr" -> "🇹🇷"
            "vi" -> "🇻🇳"
            "ta" -> "🇮🇳" // o 🇱🇰 ? Tamil è parlato in India e Sri Lanka
            "te" -> "🇮🇳"
            "mr" -> "🇮🇳"
            "ur" -> "🇵🇰" // o 🇮🇳 ? Urdu è parlato in Pakistan e India
            "fa" -> "🇮🇷"
            "gu" -> "🇮🇳"
            "pl" -> "🇵🇱"
            "uk" -> "🇺🇦"
            "my" -> "🇲🇲"
            "ml" -> "🇮🇳"
            "kn" -> "🇮🇳"
            "th" -> "🇹🇭"
            "nl" -> "🇳🇱"
            "ms" -> "🇲🇾"
            "ro" -> "🇷🇴"
            "el" -> "🇬🇷"
            "hu" -> "🇭🇺"
            "sv" -> "🇸🇪"
            "da" -> "🇩🇰"
            "fi" -> "🇫🇮"
            "no" -> "🇳🇴"
            "cs" -> "🇨🇿"
            "sk" -> "🇸🇰"
            "hr" -> "🇭🇷"
            "sr" -> "🇷🇸"
            "bg" -> "🇧🇬"
            "he" -> "🇮🇱"
            "iw" -> "🇮🇱" // Vecchio codice per ebraico
            "fil" -> "🇵🇭"
            "tl" -> "🇵🇭" // Vecchio codice per tagalog
            "yue" -> "🇭🇰" // Cantonese (Hong Kong)
            "jv" -> "🇮🇩"  // Giavanese (Indonesia)
            "ha" -> "🇳🇬"  // Hausa (Nigeria)
            else -> "🌍"
        }
    } else {
        "🌍"
    }
}

fun getLanguageName(context: Context, code: String): String {
    val supportedLanguageCodes = context.resources.getStringArray(com.gentestrana.R.array.supported_language_codes).toList()
    return if (supportedLanguageCodes.contains(code.lowercase())) {
        when (code.lowercase()) {
            "en" -> "English"
            "zh" -> "中文" // or "Chinese" if you prefer English names
            "hi" -> "हिन्दी" // or "Hindi"
            "es" -> "Español"
            "fr" -> "Français"
            "ar" -> "العربية" // or "Arabic"
            "bn" -> "বাংলা" // or "Bengali"
            "ru" -> "Русский" // or "Russian"
            "pt" -> "Português"
            "id" -> "Bahasa Indonesia" // or "Indonesian"
            "de" -> "Deutsch"
            "ja" -> "日本語" // or "Japanese"
            "sw" -> "Kiswahili" // or "Swahili"
            "ko" -> "한국어" // or "Korean"
            "it" -> "Italiano"
            "tr" -> "Türkçe" // or "Turkish"
            "vi" -> "Tiếng Việt" // or "Vietnamese"
            "ta" -> "தமிழ்" // or "Tamil"
            "te" -> "తెలుగు" // or "Telugu"
            "mr" -> "मराठी" // or "Marathi"
            "ur" -> "اردو" // or "Urdu"
            "fa" -> "فارسی" // or "Persian"
            "gu" -> "ગુજરાતી" // or "Gujarati"
            "pl" -> "Polski" // or "Polish"
            "uk" -> "Українська" // or "Ukrainian"
            "my" -> "ဗမာစာ" // or "Burmese"
            "ml" -> "മലയാളം" // or "Malayalam"
            "kn" -> "ಕನ್ನಡ" // or "Kannada"
            "th" -> "ไทย" // or "Thai"
            "nl" -> "Nederlands" // or "Dutch"
            "ms" -> "Bahasa Melayu" // or "Malay"
            "ro" -> "Română" // or "Romanian"
            "el" -> "Ελληνικά" // or "Greek"
            "hu" -> "Magyar" // or "Hungarian"
            "sv" -> "Svenska" // or "Swedish"
            "da" -> "Dansk" // or "Danish"
            "fi" -> "Suomi" // or "Finnish"
            "no" -> "Norsk" // or "Norwegian"
            "cs" -> "Čeština" // or "Czech"
            "sk" -> "Slovenčina" // or "Slovak"
            "hr" -> "Hrvatski" // or "Croatian"
            "sr" -> "Српски" // or "Serbian"
            "bg" -> "Български" // or "Bulgarian"
            "he" -> "עברית" // or "Hebrew"
            "iw" -> "עברית" // Vecchio codice per ebraico
            "fil" -> "Filipino"
            "tl" -> "Filipino" // Vecchio codice per tagalog, ora usa "fil"
            "yue" -> "粵語" // or "Cantonese"
            "jv" -> "Basa Jawa" // or "Javanese"
            "ha" -> "Hausa"
            else -> code
        }
    } else {
        code
    }
}
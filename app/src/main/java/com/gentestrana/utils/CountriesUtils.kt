package com.gentestrana.utils

import java.util.Locale

fun getCountriesList(): List<String> {
    return Locale.getISOCountries().map { countryCode ->
        Locale("", countryCode).getDisplayCountry(Locale.getDefault())
    }.distinct().sorted()
}

fun getCountryIsoFromName(countryName: String): String? {
    val normalized = countryName.trim().lowercase()
    return Locale.getISOCountries().firstOrNull { code ->
        // Confrontiamo il nome in inglese e in italiano per maggiore copertura
        Locale("", code).getDisplayCountry(Locale.ENGLISH).trim().lowercase() == normalized ||
                Locale("", code).getDisplayCountry(Locale.ITALIAN).trim().lowercase() == normalized
    }
}

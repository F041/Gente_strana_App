package com.gentestrana.utils

import com.gentestrana.users.User

/**
 * Campi che l'utente deve obbligatoriamente compilare perché il profilo sia considerato completo.
 *
 * Regola di prodotto: bio, almeno un topic e data di nascita sono obbligatori.
 */
enum class RequiredProfileField {
    BIO,
    TOPICS,
    BIRTH_DATE
}

/**
 * Restituisce l'elenco dei campi obbligatori ancora mancanti, nell'ordine in cui vanno mostrati all'utente.
 *
 * E' una funzione pura (nessuna dipendenza da Android, Firebase o Compose) cosi puo essere
 * testata con un normale test unitario. La stessa logica viene usata da:
 * - RegistrationScreen (validazione del form di registrazione)
 * - NavGraph/CompleteProfileScreen (blocco per login Google e utenti gia registrati)
 * - PersonalProfileScreen (impedisce di svuotare i campi obbligatori)
 *
 * @param bio bio dell'utente. Null o composta solo da spazi conta come mancante.
 * @param topics argomenti di interesse. Null o lista vuota conta come mancante.
 * @param rawBirthTimestamp data di nascita in millisecondi.
 *        0L e la convenzione usata in tutta l'app per indicare "data non impostata".
 *        Attenzione: un valore negativo indica una data precedente al 1970 ed e quindi una data VALIDA.
 */
fun missingRequiredProfileFields(
    bio: String?,
    topics: List<String>?,
    rawBirthTimestamp: Long
): List<RequiredProfileField> {
    val missing = mutableListOf<RequiredProfileField>()
    if (bio.isNullOrBlank()) missing.add(RequiredProfileField.BIO)
    if (topics.isNullOrEmpty()) missing.add(RequiredProfileField.TOPICS)
    if (rawBirthTimestamp == 0L) missing.add(RequiredProfileField.BIRTH_DATE)
    return missing
}

/**
 * true se bio, almeno un topic e data di nascita sono tutti presenti.
 */
fun isProfileComplete(
    bio: String?,
    topics: List<String>?,
    rawBirthTimestamp: Long
): Boolean = missingRequiredProfileFields(bio, topics, rawBirthTimestamp).isEmpty()

/**
 * Come sopra, ma applicata direttamente a un [User].
 *
 * Nota: User.birthTimestamp restituisce gia 0L quando la data non e impostata,
 * quindi l'utente che non ha mai scelto la data risulta correttamente incompleto.
 */
fun isProfileComplete(user: User): Boolean =
    isProfileComplete(user.bio, user.topics, user.birthTimestamp)
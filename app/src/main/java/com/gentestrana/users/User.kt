package com.gentestrana.users

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
/**

Modello dati per l'utente.

Nota:

Il campo [sex] accetta solo i valori "M", "F" oppure "Undefined".

Il campo [birthTimestamp] rappresenta la data di nascita in formato timestamp (millisecondi),

così l'età reale verrà calcolata dinamicamente dalla UI.

[profilePicUrl] ora è una lista di URL, con un valore di default per una foto generica.

[description] è ora una lista di stringhe, per rappresentare gli argomenti di interesse.

[spokenLanguages] è una lista di lingue parlate. Android non fornisce una lista predefinita,

quindi andrà gestita a livello di UI o logica di business.

[location] rappresenta il paese (o una stringa simile) ottenuta, ad esempio, dal GPS al primo utilizzo.
 */
data class User(
    val username: String = "",
    val bio: String = "",
    val description: List<String> = emptyList(),
    @PropertyName("profilePicUrl")
    val profilePicUrl: List<String> = listOf("https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"), // Sempre una lista
    val rawBirthTimestamp: Any? = null,
    val sex: String = "Undefined",
    val docId: String = "",
    val fcmToken: String = "",
    val spokenLanguages: List<String> = emptyList(),
    val location: String = ""
) {

    // Normalizza il campo profilePicUrl: restituisce sempre una lista di stringhe
    val normalizedProfilePicUrl: List<String>
        get() = profilePicUrl.ifEmpty {
            listOf("https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png")
        }

    // Proprietà calcolata per ottenere il timestamp in millisecondi
    val birthTimestamp: Long
        get() = when (rawBirthTimestamp) {
            is Timestamp -> rawBirthTimestamp.toDate().time
            is Long -> rawBirthTimestamp
            else -> 0L
        }
}

// Lista di sex supportata
enum class sex
{
    MALE,
    FEMALE,
    UNDEFINED,
}
package com.gentestrana.users

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.util.Calendar
import java.util.TimeZone

/**

Modello dati per l'utente.

Nota:

Il campo [sex] accetta solo i valori "M", "F" oppure "Undefined".

Il campo [birthTimestamp] rappresenta la data di nascita in formato timestamp (millisecondi),

così l'età reale verrà calcolata dinamicamente dalla UI.

[profilePicUrl] ora è una lista di URL, con un valore di default per una foto generica.

[topics] è ora una lista di stringhe, per rappresentare gli argomenti di interesse.

[spokenLanguages] è una lista di lingue parlate. Android non fornisce una lista predefinita,

quindi andrà gestita a livello di UI o logica di business.

[location] rappresenta il paese (o una stringa simile) ottenuta, ad esempio, dal GPS al primo utilizzo.
 */
data class User(
     @PropertyName("username")
    val username: String = "",
    val bio: String = "",
    val topics: List<String> = emptyList(),
     @PropertyName("profilePicUrl")
     val profilePicUrl: List<String> = emptyList(),
     @PropertyName("rawBirthTimestamp")
     val rawBirthTimestamp: Any? = null,
    val sex: String = "Undefined", // Campo per il sesso: "M", "F" o "Undefined"
    @PropertyName("docId")
     val docId: String = "",
    val fcmToken: String = "",
    val spokenLanguages: List<String> = emptyList(),
    val location: String = "",
    val isAdmin: Boolean = false,
    @PropertyName("registrationDate")
    val registrationDate: Timestamp? = null,
    @PropertyName("registrationType")
    val registrationType: String? = null,
     @PropertyName("lastActive")
    val lastActive: Timestamp? = null,
     val blockedUsers: List<String> = emptyList()
) {

    // Proprietà calcolata per ottenere il timestamp in millisecondi
    val birthTimestamp: Long
        get() = when (rawBirthTimestamp) {
            is Timestamp -> {
                // Converti il Timestamp di Firebase in Calendar UTC
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = rawBirthTimestamp.toDate().time
                }
                cal.timeInMillis // Restituisci timestamp UTC
            }
            else -> 0L
        }
}
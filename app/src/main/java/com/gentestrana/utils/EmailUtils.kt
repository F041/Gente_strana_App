package com.gentestrana.utils

import android.content.Context
import android.util.Log
import com.gentestrana.R
import com.sendgrid.Request
import com.sendgrid.SendGrid
import java.io.IOException
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Email as SendGridEmail
import com.sendgrid.helpers.mail.objects.Content as SendGridContent
import com.sendgrid.helpers.mail.objects.Personalization as SendGridPersonalization

object EmailUtils {

    fun sendReminderEmail(
        context: Context,
        recipientEmail: String,
        recipientUsername: String,
        apiKey: String
    ) {
        val sg = SendGrid(apiKey)
        val request = Request().apply {
            configureForSendGrid() // Usa l'estensione Kotlin
            body = buildEmailBody(context, recipientEmail, recipientUsername)

        }

        try {
            val response = sg.api(request)
            Log.d("PromemoriaDiagnosi", "Email promemoria inviata. Status code: ${response.statusCode}")
            if (response.statusCode >= 400) {
                Log.e("PromemoriaDiagnosi", "Errore nell'invio email promemoria. Body: ${response.body}")
            }
        } catch (ex: IOException) {
            Log.e("PromemoriaDiagnosi", "IOException durante invio email promemoria", ex)
        }
    }


    private fun buildEmailBody(
        context: Context, // Nuovo parametro
        recipientEmail: String,
        recipientUsername: String
    ): String {
        val fromEmail = SendGridEmail("noreply@gentestrana.com")
        val toEmail = SendGridEmail(recipientEmail)
        val subject = context.getString(R.string.email_subject) // Recupera l'oggetto localizzato
        val content = SendGridContent("text/plain", buildEmailText(context, recipientUsername))

        val mail = Mail(fromEmail, subject, toEmail, content)
        val personalization = SendGridPersonalization()
        personalization.addTo(toEmail)
        mail.addPersonalization(personalization)

        return mail.build()
    }


    private fun buildEmailText(context: Context, recipientUsername: String): String {
        return """
        ${context.getString(R.string.email_greeting, recipientUsername)}
        
        ${context.getString(R.string.email_body_1)}
        
        ${context.getString(R.string.email_body_2)}
        
        ${context.getString(R.string.email_body_3)}
        
        ${context.getString(R.string.email_footer, context.getString(R.string.website_url))}
        
        ${context.getString(R.string.email_signature)}
    """.trimIndent()
    }
}
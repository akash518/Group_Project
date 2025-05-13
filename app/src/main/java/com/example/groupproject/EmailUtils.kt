package com.example.groupproject

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Utility class for sending email reminders using a Google Apps Script webhook.
 */
class EmailUtils {
    /**
     * Sends an email reminder via HTTP POST.
     * @param email   Recipient email address
     * @param subject Subject of the email
     * @param text    Body text of the email
     */
    fun sendEmailReminder(email: String, subject: String, text: String) {
        // Create an OkHttpClient instance to make the network request
        val client = OkHttpClient()

        // Create an OkHttpClient instance to make the network request
        val json = JSONObject().apply {
            put("email", email)
            put("subject", subject)
            put("text", text)
        }

        // Define the request media type as JSON
        val mediaType = "application/json".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        // Convert JSON object to request body
        val request = Request.Builder()
            .url("https://script.google.com/macros/s/AKfycbyqyVAtBY478z_9gq4bsqv_en-HUQx_UYxo8UfTMu-8kYh4Va3idPlSAIRj4hIJNkij/exec")
            .post(body)
            .build()

        // Send the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EmailUtils", "Failed to send email", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyText = response.body?.string()
                Log.d("EmailUtils", "Email sent: ${response.code} — Response: $bodyText")
            }
        })
    }
}
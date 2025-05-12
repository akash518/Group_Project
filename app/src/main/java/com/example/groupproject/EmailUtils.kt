package com.example.groupproject

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class EmailUtils {
    fun sendEmailReminder(email: String, subject: String, text: String) {
        val client = OkHttpClient()

        val json = JSONObject().apply {
            put("email", email)
            put("subject", subject)
            put("text", text)
        }

        val mediaType = "application/json".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://script.google.com/macros/s/AKfycbyqyVAtBY478z_9gq4bsqv_en-HUQx_UYxo8UfTMu-8kYh4Va3idPlSAIRj4hIJNkij/exec")
            .post(body)
            .build()

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
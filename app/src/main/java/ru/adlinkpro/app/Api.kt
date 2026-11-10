package ru.adlinkpro.app

import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Api {
    private const val BASE_URL = "https://adlinkpro.ru/mobapi/"

    fun post(path: String, params: Map<String, String>, token: String?): JSONObject {
        val conn = URL(BASE_URL + path).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        // Токен дублируем в теле: часть хостингов вырезает заголовок Authorization
        val allParams = if (token != null) params + mapOf("token" to token) else params
        val body = allParams.entries.joinToString("&") {
            URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
        }

        try {
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            Log.d("Api", "HTTP $code: $text")
            return JSONObject(text.ifEmpty { "{\"ok\":false,\"error\":\"Пустой ответ сервера\"}" })
        } finally {
            conn.disconnect()
        }
    }
}

package ru.adlinkpro.app

import android.content.Context
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ApiUpload {
    private const val BASE = "https://adlinkpro.ru/mobapi/"

    fun post(
        ctx: Context,
        script: String,
        fields: Map<String, String>,
        fileField: String? = null,
        fileName: String? = null,
        fileMime: String? = null,
        fileBytes: ByteArray? = null
    ): JSONObject {
        val token = ctx.getSharedPreferences("app", Context.MODE_PRIVATE).getString("token", "") ?: ""
        return try {
            val boundary = "----AlpBoundary" + System.currentTimeMillis()
            val conn = URL(BASE + script).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            val out = DataOutputStream(conn.outputStream)
            fun write(s: String) = out.write(s.toByteArray(Charsets.UTF_8))

            // Каждая часть multipart обязана начинаться с --boundary, включая первую — без этого сервер не видит поля
            write("--$boundary\r\n")
            write("Content-Disposition: form-data; name=\"token\"\r\n\r\n$token\r\n")
            for ((k, v) in fields) {
                write("--$boundary\r\n")
                write("Content-Disposition: form-data; name=\"$k\"\r\n\r\n")
                write("$v\r\n")
            }
            if (fileField != null && fileBytes != null) {
                val safeName = (fileName ?: "banner.png").replace("\"", "").replace("\r", "").replace("\n", "")
                write("--$boundary\r\n")
                write("Content-Disposition: form-data; name=\"$fileField\"; filename=\"$safeName\"\r\n")
                write("Content-Type: ${fileMime ?: "application/octet-stream"}\r\n\r\n")
                out.write(fileBytes)
                write("\r\n")
            }
            write("--$boundary--\r\n")
            out.flush()
            out.close()

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (text.trimStart().startsWith("{")) JSONObject(text)
            else JSONObject().put("ok", false).put("error", "Сервер вернул ошибку (HTTP $code)")
        } catch (e: Exception) {
            JSONObject().put("ok", false).put("error", "Сеть недоступна: ${e.message}")
        }
    }
}

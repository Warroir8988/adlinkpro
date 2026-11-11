package ru.adlinkpro.app

import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ApiUpload {
    const val BASE = "https://adlinkpro.ru/mobapi/"

    fun upload(
        file: String,
        params: Map<String, String>,
        bytes: ByteArray,
        fileName: String,
        mime: String,
        token: String?
    ): org.json.JSONObject {
        val allParams = LinkedHashMap<String, String>(params)
        if (!token.isNullOrEmpty() && !allParams.containsKey("token")) {
            allParams["token"] = token
        }

        val boundary = "----AdLinkPro" + System.currentTimeMillis()
        val conn = (URL(BASE + file).openConnection()) as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
        if (!token.isNullOrEmpty()) conn.setRequestProperty("Authorization", "Bearer " + token)

        val out = DataOutputStream(conn.outputStream)
        for ((k, v) in allParams) {
            out.writeBytes("--" + boundary + "\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"" + k + "\"\r\n\r\n")
            out.writeBytes(v + "\r\n")
        }
        out.writeBytes("--" + boundary + "\r\n")
        out.writeBytes("Content-Disposition: form-data; name=\"banner_file\"; filename=\"" + fileName + "\"\r\n")
        out.writeBytes("Content-Type: " + mime + "\r\n\r\n")
        out.write(bytes)
        out.writeBytes("\r\n")
        out.writeBytes("--" + boundary + "--\r\n")
        out.flush()
        out.close()

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return try {
            org.json.JSONObject(if (text.isEmpty()) "{\"ok\":false,\"error\":\"HTTP " + code + "\"}" else text)
        } catch (e: Exception) {
            org.json.JSONObject("{\"ok\":false,\"error\":\"Ответ сервера не разобран\"}")
        }
    }
}

package ru.adlinkpro.app

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object Api {
    private const val BASE = "https://adlinkpro.ru/mobile/"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun post(action: String, params: Map<String, String>, token: String?): JSONObject {
        val form = FormBody.Builder()
        params.forEach { (k, v) -> form.add(k, v) }
        token?.let { form.add("token", it) }
        val req = Request.Builder().url(BASE + action).post(form.build()).build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: "{}"
            return JSONObject(body)
        }
    }
}

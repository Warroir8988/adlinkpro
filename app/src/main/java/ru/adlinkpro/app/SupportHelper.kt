package ru.adlinkpro.app

import android.content.Context
import android.content.Intent
import org.json.JSONObject

fun needLogin(ctx: Context, res: JSONObject): Boolean {
    if (res.optString("error") == "Требуется вход") {
        ctx.getSharedPreferences("app", Context.MODE_PRIVATE).edit().remove("token").apply()
        try { ctx.startActivity(Intent(ctx, LoginActivity::class.java)) } catch (e: Exception) {}
        return true
    }
    return false
}

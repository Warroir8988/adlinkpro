package ru.adlinkpro.app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/* fw-push: фоновая страховка, когда передний сервис убит системой */
class PollWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        val sp = ctx.getSharedPreferences("app", Context.MODE_PRIVATE)
        val token = sp.getString("token", null) ?: return Result.success()
        val lastId = sp.getInt("last_notif_id", 0)
        return try {
            val json = Api.post("push_poll.php", mapOf("last_id" to lastId.toString()), token)
            if (json.optBoolean("ok")) {
                val items = json.optJSONArray("items")
                val latest = json.optInt("latest_id", lastId)
                val firstRun = !sp.getBoolean("poll_seen", false)
                if (items != null && !firstRun) {
                    for (i in 0 until items.length()) {
                        val o = items.getJSONObject(i)
                        val id = o.optInt("id", 0)
                        if (id > lastId) {
                            PushHelper.show(ctx, id, o.optString("title", "AdLinkPro"), o.optString("text", ""))
                        }
                    }
                }
                if (latest > lastId) {
                    sp.edit().putInt("last_notif_id", latest).putBoolean("poll_seen", true).apply()
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

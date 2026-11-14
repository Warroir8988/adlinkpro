package ru.adlinkpro.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlin.concurrent.thread

class PollService : Service() {

    private var running = false
    private val handler = Handler(Looper.getMainLooper())
    private val intervalMs = 15000L

    private val pollRunnable = object : Runnable {
        override fun run() {
            pollOnce()
            if (running) handler.postDelayed(this, intervalMs)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        handler.removeCallbacks(pollRunnable)
        handler.postDelayed(pollRunnable, 500)
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(pollRunnable)
        super.onDestroy()
    }

    private fun startInForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("adlink_service", "Служба AdLinkPro", NotificationManager.IMPORTANCE_MIN)
            ch.description = "Обеспечивает быструю доставку уведомлений"
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, "adlink_service")
            .setContentTitle("AdLinkPro")
            .setContentText("Быстрая доставка уведомлений включена")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, 1001, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, n)
        }
    }

    private fun pollOnce() {
        val sp = getSharedPreferences("app", MODE_PRIVATE)
        val token = sp.getString("token", null)
        if (token == null) {
            stopSelf()
            return
        }
        val lastId = sp.getInt("last_notif_id", 0)
        thread {
            try {
                val json = Api.post("push_poll.php", mapOf("last_id" to lastId.toString()), token)
                if (!json.optBoolean("ok")) return@thread
                val items = json.optJSONArray("items") ?: return@thread
                val latest = json.optInt("latest_id", lastId)
                val firstRun = !sp.getBoolean("poll_seen", false)
                if (!firstRun) {
                    for (i in 0 until items.length()) {
                        val o = items.getJSONObject(i)
                        val id = o.optInt("id", 0)
                        if (id > lastId) {
                            PushHelper.show(this@PollService, id, o.optString("title", "AdLinkPro"), o.optString("text", ""))
                        }
                    }
                }
                if (latest > lastId) {
                    sp.edit().putInt("last_notif_id", latest).putBoolean("poll_seen", true).apply()
                }
            } catch (_: Exception) {
            }
        }
    }
}

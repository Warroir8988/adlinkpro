package ru.adlinkpro.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PushHelper {
    const val CHANNEL_ID = "adlink_push"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, "Уведомления AdLinkPro", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Модерация, выплаты, ответы поддержки и другие события"
            nm.createNotificationChannel(ch)
        }
    }

    fun show(context: Context, id: Int, title: String, text: String) {
        try {
            ensureChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_notifications", true)
            }
            val pi = PendingIntent.getActivity(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(id, n)
        } catch (_: Exception) {}
    }

    fun scheduleFallback(context: Context) {
        try {
            val req = PeriodicWorkRequest.Builder(PollWorker::class.java, 15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("adlink_poll_fallback", ExistingPeriodicWorkPolicy.KEEP, req)
        } catch (_: Exception) {}
    }
}

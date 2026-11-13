package ru.adlinkpro.app

import android.app.Application
import android.content.Intent
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val text = "Поток: " + t.name + "\n\n" + sw.toString()

                // Сохраняем, чтобы ошибка не потерялась, если экран не откроется
                try {
                    getSharedPreferences("crash", MODE_PRIVATE)
                        .edit().putString("last", text).putBoolean("has", true).apply()
                } catch (_: Exception) {}

                // Показываем ошибку на экране вместо мгновенного закрытия
                val intent = Intent(this, CrashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("crash_text", text)
                }
                startActivity(intent)
            } catch (_: Exception) {}
            Process.killProcess(Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }
}

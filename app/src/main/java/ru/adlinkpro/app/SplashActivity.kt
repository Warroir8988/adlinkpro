package ru.adlinkpro.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import kotlin.concurrent.thread

class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Фирменный тёмный фон с плавным градиентом
        val root = findViewById<TextView>(R.id.splash_name).parent as android.view.ViewGroup
        val bg = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#0f172a"), Color.parseColor("#1e3a8a"))
        )
        root.background = bg

        val name = findViewById<TextView>(R.id.splash_name)
        name.setTypeface(null, Typeface.BOLD)

        // Полторы секунды заставки, затем решаем, куда идти
        Handler(Looper.getMainLooper()).postDelayed({
            val sp = getSharedPreferences("app", MODE_PRIVATE)
            val token = sp.getString("token", null)
            val pin = sp.getString("pin", null)

            when {
                token == null || pin == null -> {
                    // Не зарегистрирован или пин ещё не придуман — на вход
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                else -> {
                    // Всё есть — сразу в кабинет
                    val i = Intent(this, MainActivity::class.java)
                    i.putExtra("pin_ok", true)
                    startActivity(i)
                    finish()
                }
            }
        }, 1500)
    }
}

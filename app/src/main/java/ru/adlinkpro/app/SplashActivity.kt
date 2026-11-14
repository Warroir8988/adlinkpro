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

        // Полторы секунды заставки, затем LoginActivity сам решает:
        // форма входа / придумать пин / ввести сохранённый пин
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 1500)
    }
}

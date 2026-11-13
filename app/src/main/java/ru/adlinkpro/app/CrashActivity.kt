package ru.adlinkpro.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#7f1d1d")

        val crashText = intent.getStringExtra("crash_text") ?: run {
            getSharedPreferences("crash", MODE_PRIVATE).getString("last", "") ?: ""
        }
        // Прочитали — можно стереть
        getSharedPreferences("crash", MODE_PRIVATE).edit().remove("last").remove("has").apply()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#450a0a"))
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        setContentView(root)

        root.addView(TextView(this).apply {
            text = "💥 Приложение упало с ошибкой"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(4))
        })
        root.addView(TextView(this).apply {
            text = "Сделай скриншот этого экрана и пришли его — по этому тексту ошибка чинится за один шаг."
            textSize = 13f
            setTextColor(Color.parseColor("#fecaca"))
            setPadding(0, 0, 0, dp(12))
        })

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val tv = TextView(this).apply {
            setTextIsSelectable(true)
            textSize = 11f
            setTextColor(Color.WHITE)
            typeface = Typeface.MONOSPACE
            setText(if (crashText.isBlank()) "Текст ошибки не сохранился." else crashText)
        }
        scroll.addView(tv)
        root.addView(scroll)

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(12), 0, 0) }
        btnRow.addView(TextView(this).apply {
            text = "📋 Скопировать текст"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Color.parseColor("#b91c1c"))
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(6), 0) }
            setOnClickListener {
                try {
                    val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("crash", tv.text.toString()))
                    Toast.makeText(this@CrashActivity, "Скопировано", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
            }
        })
        btnRow.addView(TextView(this).apply {
            text = "↻ Перезапустить"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Color.parseColor("#b91c1c"))
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val i = packageManager.getLaunchIntentForPackage(packageName)
                i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(i)
                finish()
            }
        })
        root.addView(btnRow)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

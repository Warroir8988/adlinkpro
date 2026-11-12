package ru.adlinkpro.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SupportListActivity : Activity() {

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun token(): String = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        scroll.addView(box)
        setContentView(scroll)

        box.addView(TextView(this).apply {
            text = "🎧 Поддержка"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
        })

        val newBtn = Button(this).apply { text = "✉️ Новое обращение" }
        newBtn.setOnClickListener { startActivity(Intent(this, TicketCreateActivity::class.java)) }
        box.addView(newBtn)

        val hint = TextView(this).apply { text = "Загрузка..."; setTextColor(Color.GRAY) }
        box.addView(hint)

        Thread {
            val res = Api.post("support.php", mapOf("action" to "list"), token())
            runOnUiThread {
                if (needLogin(this, res)) return@runOnUiThread
                if (!res.optBoolean("ok")) {
                    hint.text = res.optString("error", "Ошибка сервера")
                    return@runOnUiThread
                }
                box.removeView(hint)
                val tickets = res.optJSONArray("tickets")
                if (tickets == null || tickets.length() == 0) {
                    box.addView(TextView(this).apply {
                        text = "Обращений нет.\nСоздайте первое — мы ответим в течение 24 часов."
                        setTextColor(Color.GRAY)
                        setPadding(0, dp(24), 0, 0)
                        gravity = Gravity.CENTER
                    })
                    return@runOnUiThread
                }
                for (i in 0 until tickets.length()) {
                    val t = tickets.getJSONObject(i)
                    val statusText = when (t.optString("status")) {
                        "open" -> "🟡 Открыто"
                        "answered" -> "🟢 Есть ответ"
                        else -> "⚪ Закрыто"
                    }
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(Color.parseColor("#1E2235"))
                        setPadding(dp(14), dp(12), dp(14), dp(12))
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        lp.setMargins(0, dp(10), 0, 0)
                        layoutParams = lp
                    }
                    card.addView(TextView(this).apply {
                        text = t.optString("subject")
                        textSize = 15f
                        setTypeface(null, Typeface.BOLD)
                    })
                    card.addView(TextView(this).apply {
                        text = "$statusText · ${t.optString("created")}"
                        textSize = 12f
                        setTextColor(Color.GRAY)
                    })
                    val last = t.optJSONObject("last_msg")
                    if (last != null) {
                        card.addView(TextView(this).apply {
                            text = (if (last.optBoolean("staff")) "💬 Поддержка: " else "Вы: ") + last.optString("text")
                            textSize = 13f
                            maxLines = 2
                        })
                    }
                    val id = t.optInt("id")
                    card.setOnClickListener {
                        val i2 = Intent(this, TicketChatActivity::class.java)
                        i2.putExtra("id", id)
                        startActivity(i2)
                    }
                    box.addView(card)
                }
            }
        }.start()
    }
}

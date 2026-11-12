package ru.adlinkpro.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class TicketCreateActivity : Activity() {

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
            text = "✉️ Новое обращение"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
        })

        box.addView(TextView(this).apply { text = "Тема обращения"; setPadding(0, dp(14), 0, dp(4)) })
        val subject = EditText(this).apply { hint = "Кратко опишите суть вопроса" }
        box.addView(subject)

        box.addView(TextView(this).apply { text = "Категория"; setPadding(0, dp(14), 0, dp(4)) })
        val categories = linkedMapOf(
            "general" to "📌 Общий вопрос",
            "payment" to "💳 Вопросы по платежам",
            "withdraw" to "💸 Вопросы по выводам",
            "campaign" to "📢 Вопросы по кампаниям",
            "technical" to "🔧 Техническая проблема",
            "other" to "📎 Другое"
        )
        val catSpinner = Spinner(this)
        catSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories.values.toList())
        box.addView(catSpinner)

        box.addView(TextView(this).apply { text = "Приоритет"; setPadding(0, dp(14), 0, dp(4)) })
        val priorities = linkedMapOf("normal" to "🟡 Обычный", "low" to "🟢 Низкий", "high" to "🔴 Высокий")
        val priSpinner = Spinner(this)
        priSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, priorities.values.toList())
        box.addView(priSpinner)

        box.addView(TextView(this).apply { text = "Сообщение"; setPadding(0, dp(14), 0, dp(4)) })
        val message = EditText(this).apply {
            hint = "Подробно опишите вашу проблему или вопрос..."
            minLines = 6
            gravity = Gravity.TOP
        }
        box.addView(message)

        val status = TextView(this).apply { setTextColor(Color.RED); setPadding(0, dp(8), 0, 0) }
        box.addView(status)

        val send = Button(this).apply { text = "📤 Отправить обращение" }
        box.addView(send)

        send.setOnClickListener {
            val subj = subject.text.toString().trim()
            val msg = message.text.toString().trim()
            if (subj.length < 3) { status.text = "Тема слишком короткая"; return@setOnClickListener }
            if (msg.length < 10) { status.text = "Опишите проблему подробнее (минимум 10 символов)"; return@setOnClickListener }

            send.isEnabled = false
            status.text = ""
            val catKey = categories.keys.elementAt(catSpinner.selectedItemPosition)
            val priKey = priorities.keys.elementAt(priSpinner.selectedItemPosition)

            Thread {
                val res = Api.post("support.php", mapOf(
                    "action" to "create",
                    "subject" to subj,
                    "category" to catKey,
                    "priority" to priKey,
                    "message" to msg
                ), token())
                runOnUiThread {
                    if (needLogin(this, res)) return@runOnUiThread
                    if (res.optBoolean("ok")) {
                        Toast.makeText(this, res.optString("message", "Обращение создано"), Toast.LENGTH_LONG).show()
                        val i2 = Intent(this, TicketChatActivity::class.java)
                        i2.putExtra("id", res.optInt("id"))
                        startActivity(i2)
                        finish()
                    } else {
                        status.text = res.optString("error", "Ошибка")
                        send.isEnabled = true
                    }
                }
            }.start()
        }
    }
}

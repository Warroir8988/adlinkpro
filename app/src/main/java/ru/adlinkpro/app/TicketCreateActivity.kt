package ru.adlinkpro.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class TicketCreateActivity : Activity() {

    companion object {
        val BG = Color.parseColor("#f1f5f9")
        val CARD = Color.WHITE
        val BORDER = Color.parseColor("#e2e8f0")
        val TEXT = Color.parseColor("#1e293b")
        val MUTED = Color.parseColor("#64748b")
        val PRIMARY = Color.parseColor("#3498db")
        val DANGER = Color.parseColor("#ef4444")
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun token(): String = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""

    private val categories = linkedMapOf(
        "general" to "📌 Общий вопрос",
        "payment" to "💳 Вопросы по платежам",
        "withdraw" to "💸 Вопросы по выводам",
        "campaign" to "📢 Вопросы по кампаниям",
        "technical" to "🔧 Техническая проблема",
        "other" to "📎 Другое"
    )
    private val priorities = linkedMapOf(
        "normal" to "🟡 Обычный",
        "low" to "🟢 Низкий",
        "high" to "🔴 Высокий"
    )
    private var catIndex = 0
    private var priIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = CARD

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG) }
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scroll.addView(box)
        root.addView(scroll)
        setContentView(root)

        box.addView(TextView(this).apply {
            text = "🎧 Новое обращение в поддержку"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT)
        })
        box.addView(TextView(this).apply {
            text = "Опишите вашу проблему — мы ответим в течение 24 часов."
            textSize = 13f
            setTextColor(MUTED)
            setPadding(0, dp(4), 0, dp(16))
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(CARD)
                setStroke(dp(1), BORDER)
            }
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        box.addView(card)

        fun label(text: String) = TextView(this).apply {
            this.text = text
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT)
            setPadding(0, dp(10), 0, dp(4))
        }

        fun fieldBg() = GradientDrawable().apply {
            cornerRadius = dp(6).toFloat()
            setColor(BG)
            setStroke(dp(1), BORDER)
        }

        card.addView(label("Тема обращения"))
        val subject = EditText(this).apply {
            hint = "Кратко опишите суть вопроса"
            textSize = 14f
            setTextColor(TEXT)
            setHintTextColor(MUTED)
            background = fieldBg()
            setPadding(dp(12), dp(12), dp(12), dp(12))
            filters = arrayOf(android.text.InputFilter.LengthFilter(255))
        }
        card.addView(subject)

        // Категория и приоритет в два столбца, как на сайте
        val grid = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val colLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        colLp.setMargins(0, 0, dp(8), 0)
        val colL = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val colR = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        colL.addView(label("Категория"))
        val catField = TextView(this).apply {
            text = categories.values.first()
            textSize = 14f
            setTextColor(TEXT)
            background = fieldBg()
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        catField.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Категория")
                .setSingleChoiceItems(categories.values.toTypedArray(), catIndex) { d, which ->
                    catIndex = which
                    catField.text = categories.values.elementAt(which)
                    d.dismiss()
                }
                .show()
        }
        colL.addView(catField)

        colR.addView(label("Приоритет"))
        val priField = TextView(this).apply {
            text = priorities.values.first()
            textSize = 14f
            setTextColor(TEXT)
            background = fieldBg()
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        priField.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Приоритет")
                .setSingleChoiceItems(priorities.values.toTypedArray(), priIndex) { d, which ->
                    priIndex = which
                    priField.text = priorities.values.elementAt(which)
                    d.dismiss()
                }
                .show()
        }
        colR.addView(priField)

        colL.layoutParams = colLp
        val colRp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        colR.layoutParams = colRp
        grid.addView(colL); grid.addView(colR)
        card.addView(grid)

        card.addView(label("Сообщение"))
        val message = EditText(this).apply {
            hint = "Подробно опишите вашу проблему или вопрос..."
            textSize = 14f
            minLines = 8
            gravity = Gravity.TOP
            setTextColor(TEXT)
            setHintTextColor(MUTED)
            background = fieldBg()
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        card.addView(message)

        val status = TextView(this).apply { setTextColor(DANGER); textSize = 13f; setPadding(0, dp(10), 0, 0) }
        card.addView(status)

        val btnRow = LinearLayout(this).apply { setPadding(0, dp(14), 0, 0) }
        val send = TextView(this).apply {
            text = "📤 Отправить обращение"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(PRIMARY) }
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val cancel = TextView(this).apply {
            text = "Отмена"
            textSize = 14f
            setTextColor(TEXT)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(CARD)
                setStroke(dp(1), BORDER)
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(10), 0, 0, 0)
            layoutParams = lp
        }
        cancel.setOnClickListener { finish() }
        btnRow.addView(send)
        btnRow.addView(cancel)
        card.addView(btnRow)

        send.setOnClickListener {
            val subj = subject.text.toString().trim()
            val msg = message.text.toString().trim()
            if (subj.length < 3) { status.text = "Тема слишком короткая"; return@setOnClickListener }
            if (msg.length < 10) { status.text = "Опишите проблему подробнее (минимум 10 символов)"; return@setOnClickListener }

            send.isEnabled = false
            status.text = ""
            val catKey = categories.keys.elementAt(catIndex)
            val priKey = priorities.keys.elementAt(priIndex)

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

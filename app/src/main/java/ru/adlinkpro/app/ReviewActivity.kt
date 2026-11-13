package ru.adlinkpro.app

import android.app.Activity
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
import org.json.JSONObject

class ReviewActivity : Activity() {

    private fun dp(v: Int) = Ui.dp(this, v)
    private fun token(): String = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
    private lateinit var status: TextView
    private lateinit var send: TextView
    private val stars = ArrayList<TextView>()
    private var rating = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.CARD
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.BG) }
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24)) }
        scroll.addView(box)
        root.addView(scroll)
        setContentView(root)
        load(box)
    }

    private fun load(box: LinearLayout) {
        box.addView(Ui.muted(this, "Загрузка...", 24))
        Thread {
            val res = Api.post("review.php", mapOf("action" to "get"), token())
            runOnUiThread {
                box.removeAllViews()
                render(box, res)
            }
        }.start()
    }

    private fun render(box: LinearLayout, res: JSONObject) {
        if (needLogin(this, res)) return

        box.addView(TextView(this).apply {
            text = "⭐ Отзыв о платформе"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(12))
        })

        val card = Ui.card(this)

        // Существующий отзыв
        val existing = res.optJSONObject("review")
        if (existing != null) {
            val st = existing.optString("status")
            val (label, color) = when (st) {
                "approved" -> "✅ Опубликован" to Ui.SUCCESS
                "rejected" -> "❌ Отклонён" to Ui.DANGER
                else -> "⏳ На модерации" to Ui.WARNING
            }
            card.addView(TextView(this).apply {
                text = "Ваш отзыв:"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Ui.TEXT)
                setPadding(0, 0, 0, dp(6))
            })
            card.addView(TextView(this).apply {
                text = "★".repeat(existing.optInt("rating", 5))
                textSize = 18f
                setTextColor(Color.parseColor("#f59e0b"))
                setPadding(0, 0, 0, dp(6))
            })
            card.addView(TextView(this).apply {
                text = existing.optString("text")
                textSize = 14f
                setTextColor(Ui.TEXT)
                setPadding(0, 0, 0, dp(6))
            })
            card.addView(TextView(this).apply {
                text = "$label · ${existing.optString("date")}"
                textSize = 13f
                setTextColor(color)
                setPadding(0, 0, 0, dp(6))
            })
            val comment = existing.optString("admin_comment", "")
            if (comment.isNotBlank()) {
                card.addView(TextView(this).apply {
                    text = "Комментарий поддержки: $comment"
                    textSize = 13f
                    setTextColor(Ui.MUTED)
                    setPadding(0, 0, 0, dp(10))
                })
            }
            card.addView(TextView(this).apply {
                text = "Хотите изменить отзыв? Заполните форму ниже — он снова уйдёт на модерацию."
                textSize = 12f
                setTextColor(Ui.MUTED)
                setPadding(0, 0, 0, dp(10))
            })
        }

        // Выбор оценки
        card.addView(TextView(this).apply {
            text = "Ваша оценка"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(6))
        })
        val starRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, dp(10)) }
        for (i in 1..5) {
            val s = TextView(this).apply {
                text = "☆"
                textSize = 34f
                setTextColor(Ui.MUTED)
                setPadding(0, 0, dp(10), 0)
            }
            s.setOnClickListener {
                rating = i
                stars.forEachIndexed { idx, tv -> tv.text = if (idx < i) "★" else "☆" }
                stars.forEachIndexed { idx, tv -> tv.setTextColor(if (idx < i) Color.parseColor("#f59e0b") else Ui.MUTED) }
            }
            stars.add(s)
            starRow.addView(s)
        }
        card.addView(starRow)

        // Текст отзыва
        card.addView(TextView(this).apply {
            text = "Текст отзыва"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(6))
        })
        val text = EditText(this).apply {
            hint = "Расскажите, что вам нравится или что можно улучшить..."
            textSize = 14f
            minLines = 5
            gravity = Gravity.TOP
            setTextColor(Ui.TEXT)
            setHintTextColor(Ui.MUTED)
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Ui.BG)
                setStroke(dp(1), Ui.BORDER)
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
            filters = arrayOf(android.text.InputFilter.LengthFilter(1000))
        }
        card.addView(text)

        status = TextView(this).apply { setTextColor(Ui.DANGER); textSize = 13f; setPadding(0, dp(10), 0, 0) }
        card.addView(status)

        send = Ui.btnPrimary(this, "📤 Отправить отзыв") {
            if (rating == 0) { status.text = "Поставьте оценку звёздами"; return@btnPrimary }
            val t = text.text.toString().trim()
            if (t.length < 10) { status.text = "Отзыв слишком короткий (минимум 10 символов)"; return@btnPrimary }
            send.isEnabled = false
            status.text = ""
            Thread {
                val r2 = Api.post("review.php", mapOf("action" to "save", "rating" to rating.toString(), "text" to t), token())
                runOnUiThread {
                    send.isEnabled = true
                    if (r2.optBoolean("ok")) {
                        Toast.makeText(this, r2.optString("message", "Отзыв отправлен"), Toast.LENGTH_LONG).show()
                        load(box)
                    } else {
                        status.text = r2.optString("error", "Ошибка")
                    }
                }
            }.start()
        }
        card.addView(send, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(14), 0, 0) })

        box.addView(card)
    }
}

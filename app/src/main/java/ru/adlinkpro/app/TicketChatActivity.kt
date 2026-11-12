package ru.adlinkpro.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

class TicketChatActivity : Activity() {

    companion object {
        val BG = Color.parseColor("#f1f5f9")
        val CARD = Color.WHITE
        val BORDER = Color.parseColor("#e2e8f0")
        val TEXT = Color.parseColor("#1e293b")
        val MUTED = Color.parseColor("#64748b")
        val PRIMARY = Color.parseColor("#3498db")
        val WARNING = Color.parseColor("#f59e0b")
        val BLUE = Color.parseColor("#1e40af")
        val BLUE_BG = Color.parseColor("#dbeafe")
        val GREEN = Color.parseColor("#059669")
        val GREEN_BG = Color.parseColor("#d1fae5")
        val YELLOW_BG = Color.parseColor("#fef3c7")
        val STAFF_BG = Color.parseColor("#eef2ff")
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun token(): String = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
    private var ticketId = 0
    private lateinit var box: LinearLayout
    private lateinit var inputWrap: LinearLayout
    private lateinit var input: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = CARD
        ticketId = intent.getIntExtra("id", 0)
        if (ticketId == 0) { finish(); return }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG) }
        val scroll = ScrollView(this)
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        scroll.addView(box)
        inputWrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(dp(12), dp(8), dp(12), dp(12))
            setBackgroundColor(CARD)
        }
        input = EditText(this).apply {
            hint = "Ваше сообщение..."
            textSize = 14f
            setTextColor(TEXT)
            setHintTextColor(MUTED)
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(BG)
                setStroke(dp(1), BORDER)
            }
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val send = Button(this).apply { text = "➤" }
        inputWrap.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        inputWrap.addView(send)
        inputWrap.visibility = View.GONE
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(inputWrap)
        setContentView(root)

        status = TextView(this).apply { text = "Загрузка..."; setTextColor(MUTED); gravity = Gravity.CENTER; setPadding(0, dp(32), 0, 0) }
        box.addView(status)

        load()

        send.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.length < 2) return@setOnClickListener
            send.isEnabled = false
            Thread {
                val res = Api.post("support.php", mapOf("action" to "reply", "id" to ticketId.toString(), "message" to text), token())
                runOnUiThread {
                    send.isEnabled = true
                    if (res.optBoolean("ok")) {
                        input.setText("")
                        load()
                    } else {
                        Toast.makeText(this, res.optString("error", "Ошибка"), Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    private fun load() {
        Thread {
            val res = Api.post("support.php", mapOf("action" to "view", "id" to ticketId.toString()), token())
            runOnUiThread { render(res) }
        }.start()
    }

    private fun render(res: JSONObject) {
        if (needLogin(this, res)) return
        if (!res.optBoolean("ok")) {
            status.text = res.optString("error", "Ошибка")
            return
        }
        box.removeAllViews()
        val t = res.getJSONObject("ticket")
        val closed = t.optString("status") in listOf("closed", "resolved")

        val (badge, badgeColor, badgeBg) = when (t.optString("status")) {
            "open" -> Triple("🟡 Открыто", WARNING, YELLOW_BG)
            "in_progress" -> Triple("🔵 В работе", BLUE, BLUE_BG)
            "closed" -> Triple("✅ Закрыто", GREEN, GREEN_BG)
            "answered" -> Triple("🟢 Есть ответ", GREEN, GREEN_BG)
            else -> Triple(t.optString("status"), MUTED, BG)
        }

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(TextView(this).apply {
            text = "#${t.optInt("id")} · ${t.optString("subject")}"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        head.addView(TextView(this).apply {
            text = badge
            textSize = 11f
            setTextColor(badgeColor)
            background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(badgeBg) }
            setPadding(dp(8), dp(3), dp(8), dp(3))
        })
        box.addView(head)
        box.addView(TextView(this).apply {
            text = "Создано ${t.optString("created")}"
            textSize = 12f
            setTextColor(MUTED)
            setPadding(0, dp(4), 0, dp(12))
        })

        val msgs = t.optJSONArray("messages") ?: return
        for (i in 0 until msgs.length()) {
            val m = msgs.getJSONObject(i)
            val staff = m.optBoolean("staff")
            val bubble = TextView(this).apply {
                text = m.optString("text") + "\n" + m.optString("date")
                textSize = 14f
                setTextColor(TEXT)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(if (staff) STAFF_BG else BLUE_BG)
                }
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, dp(6), 0, 0)
                lp.gravity = if (staff) Gravity.START else Gravity.END
                layoutParams = lp
            }
            box.addView(bubble)
        }

        if (closed) {
            box.addView(TextView(this).apply {
                text = "Обращение закрыто. Создайте новое, если вопрос остался."
                setTextColor(MUTED)
                setPadding(0, dp(12), 0, 0)
            })
        } else {
            inputWrap.visibility = View.VISIBLE
        }
    }
}

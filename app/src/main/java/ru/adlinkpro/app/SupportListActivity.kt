package ru.adlinkpro.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class SupportListActivity : Activity() {

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
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun token(): String = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""

    private lateinit var box: LinearLayout
    private var currentStatus = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = CARD

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
        }
        val scroll = ScrollView(this)
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scroll.addView(box)
        root.addView(scroll)
        setContentView(root)

        load()
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            cornerRadius = dp(6).toFloat()
            setColor(CARD)
            setStroke(dp(1), BORDER)
        }
        setPadding(dp(14), dp(12), dp(14), dp(12))
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, 0, 0, dp(14))
        layoutParams = lp
    }

    private fun primaryBtn(text: String, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setTypeface(null, Typeface.BOLD)
        background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(PRIMARY) }
        setPadding(dp(16), dp(10), dp(16), dp(10))
        setOnClickListener { onClick() }
    }

    private fun ghostBtn(text: String, active: Boolean, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        textSize = 13f
        gravity = Gravity.CENTER
        if (active) {
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(PRIMARY) }
        } else {
            setTextColor(TEXT)
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(CARD)
                setStroke(dp(1), BORDER)
            }
        }
        setPadding(dp(12), dp(8), dp(12), dp(8))
        setOnClickListener { onClick() }
    }

    private fun load() {
        box.removeAllViews()

        // Заголовок + кнопка, как на сайте: в одну строку
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(TextView(this).apply {
            text = "🎧 Мои обращения"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT)
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
        })
        val newBtn = primaryBtn("+ Новое обращение") {
            startActivity(Intent(this, TicketCreateActivity::class.java))
        }
        head.addView(newBtn)
        box.addView(head)

        // Статистика: три плитки в один ряд
        val statsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val statsLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        statsLp.setMargins(0, dp(14), dp(6), 0)
        val st1 = TextView(this).apply {
            text = "0\nВсего обращений"
            textSize = 14f
            setTextColor(TEXT)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(CARD); setStroke(dp(1), BORDER) }
        }
        val st2 = TextView(this).apply {
            text = "0\nОткрытых"
            textSize = 14f
            setTextColor(WARNING)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(CARD); setStroke(dp(1), BORDER) }
        }
        val st3 = TextView(this).apply {
            text = "0\nЗакрытых"
            textSize = 14f
            setTextColor(GREEN)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(CARD); setStroke(dp(1), BORDER) }
        }
        st1.layoutParams = statsLp
        val statsLp2 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        statsLp2.setMargins(0, dp(14), dp(6), 0)
        st2.layoutParams = statsLp2
        val statsLp3 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        statsLp3.setMargins(0, dp(14), 0, 0)
        st3.layoutParams = statsLp3
        statsRow.addView(st1); statsRow.addView(st2); statsRow.addView(st3)
        box.addView(statsRow)

        // Фильтры — как на сайте
        val filters = card()
        val fRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fRow.addView(ghostBtn("Все", currentStatus == "all") { currentStatus = "all"; load() },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(6), 0) })
        fRow.addView(ghostBtn("🟡 Открытые", currentStatus == "open") { currentStatus = "open"; load() },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(6), 0) })
        fRow.addView(ghostBtn("🔵 В работе", currentStatus == "in_progress") { currentStatus = "in_progress"; load() },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(6), 0) })
        fRow.addView(ghostBtn("✅ Закрытые", currentStatus == "closed") { currentStatus = "closed"; load() },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        filters.addView(fRow)
        box.addView(filters)

        val hint = TextView(this).apply {
            text = "Загрузка..."
            setTextColor(MUTED)
            gravity = Gravity.CENTER
            setPadding(0, dp(32), 0, 0)
        }
        box.addView(hint)

        Thread {
            val res = Api.post("support.php", mapOf("action" to "list", "status" to currentStatus), token())
            runOnUiThread {
                if (needLogin(this, res)) return@runOnUiThread
                if (!res.optBoolean("ok")) {
                    hint.text = res.optString("error", "Ошибка сервера")
                    return@runOnUiThread
                }
                box.removeView(hint)
                val tickets: JSONArray = res.optJSONArray("tickets") ?: JSONArray()

                // Статистика по сайту: всего / открытых (open + in_progress) / закрытых
                var opened = 0
                var closedN = 0
                for (i in 0 until tickets.length()) {
                    val s = tickets.getJSONObject(i).optString("status")
                    if (s == "closed") closedN++ else opened++
                }
                st1.text = "${tickets.length()}\nВсего обращений"
                st2.text = "$opened\nОткрытых"
                st3.text = "$closedN\nЗакрытых"

                if (tickets.length() == 0) {
                    val empty = card()
                    empty.gravity = Gravity.CENTER
                    empty.setPadding(dp(20), dp(60), dp(20), dp(20))
                    empty.addView(TextView(this).apply {
                        text = "📭"
                        textSize = 48f
                        gravity = Gravity.CENTER
                    })
                    empty.addView(TextView(this).apply {
                        text = "У вас пока нет обращений"
                        textSize = 18f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(TEXT)
                        gravity = Gravity.CENTER
                        setPadding(0, dp(12), 0, dp(4))
                    })
                    empty.addView(TextView(this).apply {
                        text = "Создайте обращение, если у вас есть вопрос или проблема"
                        textSize = 14f
                        setTextColor(MUTED)
                        gravity = Gravity.CENTER
                        setPadding(0, 0, 0, dp(16))
                    })
                    val createBtn = primaryBtn("+ Создать обращение") {
                        startActivity(Intent(this, TicketCreateActivity::class.java))
                    }
                    val wrap = LinearLayout(this).apply { gravity = Gravity.CENTER }
                    wrap.addView(createBtn)
                    empty.addView(wrap)
                    box.addView(empty)
                    return@runOnUiThread
                }

                for (i in 0 until tickets.length()) {
                    val t = tickets.getJSONObject(i)
                    val (badge, badgeColor, badgeBg) = when (t.optString("status")) {
                        "open" -> Triple("🟡 Открыто", WARNING, YELLOW_BG)
                        "in_progress" -> Triple("🔵 В работе", BLUE, BLUE_BG)
                        "closed" -> Triple("✅ Закрыто", GREEN, GREEN_BG)
                        "answered" -> Triple("🟢 Есть ответ", GREEN, GREEN_BG)
                        else -> Triple(t.optString("status"), MUTED, BG)
                    }
                    val item = card()
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                    row.addView(TextView(this@SupportListActivity).apply {
                        text = t.optString("subject")
                        textSize = 15f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(TEXT)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    })
                    row.addView(TextView(this@SupportListActivity).apply {
                        text = badge
                        textSize = 11f
                        setTextColor(badgeColor)
                        background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(badgeBg) }
                        setPadding(dp(8), dp(3), dp(8), dp(3))
                    })
                    item.addView(row)
                    item.addView(TextView(this).apply {
                        text = t.optString("created")
                        textSize = 12f
                        setTextColor(MUTED)
                        setPadding(0, dp(3), 0, 0)
                    })
                    val last = t.optJSONObject("last_msg")
                    if (last != null) {
                        item.addView(TextView(this).apply {
                            text = (if (last.optBoolean("staff")) "💬 Поддержка: " else "Вы: ") + last.optString("text")
                            textSize = 13f
                            setTextColor(MUTED)
                            maxLines = 2
                            setPadding(0, dp(6), 0, 0)
                        })
                    }
                    val id = t.optInt("id")
                    item.setOnClickListener {
                        val i2 = Intent(this, TicketChatActivity::class.java)
                        i2.putExtra("id", id)
                        startActivity(i2)
                    }
                    box.addView(item)
                }
            }
        }.start()
    }
}

package ru.adlinkpro.app

import android.app.Activity
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RatingActivity : Activity() {

    private fun dp(v: Int) = Ui.dp(this, v)
    private fun token(): String = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
    private lateinit var box: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.CARD
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.BG) }
        val scroll = ScrollView(this)
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24)) }
        scroll.addView(box)
        root.addView(scroll)
        setContentView(root)
        box.addView(Ui.muted(this, "Загрузка...", 24))
        load()
    }

    private fun load() {
        Thread {
            val res = Api.post("referral.php", mapOf("action" to "rating"), token())
            runOnUiThread { render(res) }
        }.start()
    }

    private fun render(res: JSONObject) {
        if (needLogin(this, res)) return
        box.removeAllViews()
        box.addView(TextView(this).apply {
            text = "🏆 Рейтинг рефереров"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(16))
        })
        if (!res.optBoolean("ok")) {
            box.addView(TextView(this).apply { text = res.optString("error", "Ошибка сервера"); textSize = 14f; setTextColor(Ui.DANGER); setPadding(0, dp(8), 0, 0) })
            return
        }

        // Жёлтый призовой блок — градиент и цвета как на сайте
        val prizesCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#fef3c7"), Color.parseColor("#fde68a"))).apply {
                cornerRadius = dp(6).toFloat()
                setStroke(dp(1), Color.parseColor("#f59e0b"))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        prizesCard.addView(TextView(this).apply {
            text = "🎁 Призы за топ-места"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#78350f"))
            setPadding(0, 0, 0, dp(8))
        })
        val month = SimpleDateFormat("LLLL", Locale("ru")).format(java.util.Date())
        val year = Calendar.getInstance().get(Calendar.YEAR)
        prizesCard.addView(TextView(this).apply {
            text = "Период: Текущий месяц ($month $year)"
            textSize = 13f
            setTextColor(Color.parseColor("#92400e"))
        })
        val prizeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(12), 0, 0) }
        val prizes = res.optJSONArray("prizes") ?: JSONArray()
        val medals = listOf("🥇", "🥈", "🥉")
        val words = listOf("1 место", "2 место", "3 место")
        for (i in 0 until 3) {
            val p = if (i < prizes.length()) prizes.getJSONObject(i) else null
            val amount = p?.optString("amount") ?: when (i) { 0 -> "500,00 ₽"; 1 -> "250,00 ₽"; else -> "100,00 ₽" }
            prizeRow.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(Ui.CARD)
                setPadding(dp(10), dp(10), dp(10), dp(10))
                addView(TextView(this@RatingActivity).apply { text = medals[i]; textSize = 24f; gravity = Gravity.CENTER })
                addView(TextView(this@RatingActivity).apply { text = words[i]; textSize = 12f; setTextColor(Ui.MUTED); gravity = Gravity.CENTER })
                addView(TextView(this@RatingActivity).apply {
                    text = amount
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(if (i == 1) Color.parseColor("#6b7280") else Color.parseColor("#b45309"))
                    gravity = Gravity.CENTER
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { if (i < 2) setMargins(0, 0, dp(12), 0) })
        }
        prizesCard.addView(prizeRow)
        prizesCard.addView(TextView(this).apply {
            text = "💡 Призы начисляются автоматически в конце периода на основной баланс (можно вывести)."
            textSize = 12f
            setTextColor(Color.parseColor("#78350f"))
            setPadding(0, dp(12), 0, 0)
        })
        box.addView(prizesCard)

        // Топ-20
        val topCard = Ui.card(this)
        topCard.addView(TextView(this).apply {
            text = "🏅 Топ-20 рефереров"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(12))
        })
        val top = res.optJSONArray("top") ?: JSONArray()
        if (top.length() == 0) {
            topCard.addView(Ui.muted(this, "Пока нет участников", 30))
        } else {
            for (i in 0 until top.length()) {
                val t = top.getJSONObject(i)
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
                row.addView(TextView(this).apply {
                    text = when (t.optInt("place")) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "${t.optInt("place")}." }
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(dp(36), LinearLayout.LayoutParams.WRAP_CONTENT)
                })
                row.addView(TextView(this).apply {
                    text = t.optString("user")
                    textSize = 14f
                    setTextColor(Ui.TEXT)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(this).apply {
                    text = t.optString("earned", "0,00 ₽")
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Ui.SUCCESS)
                })
                topCard.addView(row)
            }
        }
        box.addView(topCard)
    }
}

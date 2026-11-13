package ru.adlinkpro.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.json.JSONArray
import org.json.JSONObject

class StatsActivity : Activity() {

    private fun dp(v: Int) = Ui.dp(this, v)
    private fun token(): String = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
    private lateinit var box: LinearLayout
    private var period = "7"
    private val periods = linkedMapOf("7" to "7 дней", "30" to "30 дней", "all" to "Все время")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.CARD
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.BG) }
        val scroll = ScrollView(this)
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24)) }
        scroll.addView(box)
        root.addView(scroll)
        setContentView(root)
        load()
    }

    private fun load() {
        box.removeAllViews()
        // Шапка: заголовок + выбор периода (как селект на сайте)
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(TextView(this).apply {
            text = "📊 Статистика"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val periodField = TextView(this).apply {
            text = periods[period]
            textSize = 13f
            setTextColor(Ui.TEXT)
            gravity = Gravity.CENTER
            setPadding(Ui.dp(this@StatsActivity, 12), Ui.dp(this@StatsActivity, 8), Ui.dp(this@StatsActivity, 12), Ui.dp(this@StatsActivity, 8))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Ui.CARD)
                setStroke(dp(1), Ui.BORDER)
            }
        }
        periodField.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Период")
                .setSingleChoiceItems(periods.values.toTypedArray(), periods.keys.indexOf(period)) { d, which ->
                    period = periods.keys.elementAt(which)
                    d.dismiss()
                    load()
                }
                .show()
        }
        head.addView(periodField)
        box.addView(head)
        box.addView(Ui.muted(this, "Загрузка...", 24))

        Thread {
            val res = Api.post("stats.php", mapOf("period" to period), token())
            runOnUiThread { render(res) }
        }.start()
    }

    private fun render(res: JSONObject) {
        if (needLogin(this, res)) return
        box.removeViewAt(1) // убираем «Загрузка...»
        if (!res.optBoolean("ok")) {
            box.addView(TextView(this).apply { text = res.optString("error", "Ошибка сервера"); textSize = 14f; setTextColor(Ui.DANGER); setPadding(0, dp(8), 0, 0) })
            return
        }

        // Советы — зелёные блоки с левой полосой, как на сайте
        val tips = res.optJSONArray("tips") ?: JSONArray()
        if (tips.length() > 0) {
            val tipsWrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(4)) }
            for (i in 0 until tips.length()) {
                val t = tips.getJSONObject(i)
                val tip = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundColor(Color.parseColor("#d1fae5"))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(8)) }
                }
                val strip = View(this)
                strip.setBackgroundColor(Color.parseColor("#10b981"))
                strip.layoutParams = LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT)
                val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)) }
                inner.addView(TextView(this).apply {
                    text = t.optString("icon") + " " + t.optString("title")
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#065f46"))
                })
                inner.addView(TextView(this).apply {
                    text = t.optString("text")
                    textSize = 13f
                    setTextColor(Color.parseColor("#065f46"))
                    setPadding(0, dp(3), 0, 0)
                })
                tip.addView(strip)
                tip.addView(inner, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
                tipsWrap.addView(tip)
            }
            box.addView(tipsWrap)
        }

        statCard("💼 Как рекламодатель", res.optJSONObject("adv"), listOf("Показов", "Кликов", "CTR", "Потрачено"), listOf(Ui.TEXT, Ui.TEXT, Ui.SUCCESS, Ui.DANGER))
        statCard("🌐 Как веб-мастер", res.optJSONObject("web"), listOf("Показов", "Кликов", "CTR", "Заработано"), listOf(Ui.TEXT, Ui.TEXT, Ui.SUCCESS, Ui.SUCCESS))
        placementsCard(res.optJSONArray("placements"))
        chartCard(res.optJSONArray("chart"))
    }

    private fun statCard(title: String, data: JSONObject?, labels: List<String>, colors: List<Int>) {
        val c = Ui.card(this)
        c.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(12))
        })
        if (data == null) { c.addView(Ui.muted(this, "Нет данных", 10)); box.addView(c); return }
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, 0) }
        val vals = listOf("imp", "clk", "ctr", "money")
        for (i in 0 until 4) {
            val row = if (i < 2) row1 else row2
            row.addView(Ui.statTile(this, data.optString(vals[i], "0"), labels[i], colors[i]),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(6), 0) })
        }
        c.addView(row1)
        c.addView(row2)
        box.addView(c)
    }

    private fun placementsCard(arr: JSONArray?) {
        val c = Ui.card(this)
        c.addView(TextView(this).apply {
            text = "📢 Мои баннеры и текстовые объявления"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(12))
        })
        val data = arr ?: JSONArray()
        if (data.length() == 0) {
            c.addView(Ui.muted(this, "Нет размещений", 10))
            box.addView(c)
            return
        }
        // Сводные плитки
        var imp = 0; var clk = 0
        for (i in 0 until data.length()) {
            val o = data.getJSONObject(i)
            imp += o.optInt("imp")
            clk += o.optInt("clk")
        }
        val ctr = if (imp > 0) String.format(java.util.Locale.US, "%.2f%%", clk * 100.0 / imp) else "0%"
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(Ui.statTile(this, imp.toString(), "Показов"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(6), 0) })
        row.addView(Ui.statTile(this, clk.toString(), "Кликов"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(6), 0) })
        row.addView(Ui.statTile(this, ctr, "CTR", Ui.SUCCESS))
        c.addView(row)

        // Таблица: заголовок + строки с разделителями
        fun headerCell(text: String, w: Float): TextView = TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Ui.MUTED)
            setPadding(dp(4), dp(6), dp(4), dp(6))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(10), 0, 0) }
        header.addView(headerCell("Размещение", 2.2f))
        header.addView(headerCell("Показов", 1f))
        header.addView(headerCell("Кликов", 1f))
        header.addView(headerCell("Статус", 1.1f))
        c.addView(header)
        for (i in 0 until data.length()) {
            val o = data.getJSONObject(i)
            val div = View(this)
            div.setBackgroundColor(Ui.BORDER)
            div.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
            c.addView(div)
            val status = when (o.optString("status")) {
                "active" -> "активно"
                "expired" -> "просрочено"
                else -> o.optString("status")
            }
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            fun cell(text: String, w: Float, color: Int = Ui.TEXT) = TextView(this@StatsActivity).apply {
                this.text = text
                textSize = 13f
                setTextColor(color)
                setPadding(dp(4), dp(6), dp(4), dp(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
            }
            r.addView(cell(o.optString("name") + " " + o.optString("typeNote", ""), 2.2f))
            r.addView(cell(o.optInt("imp").toString(), 1f))
            r.addView(cell(o.optInt("clk").toString(), 1f))
            r.addView(cell(status, 1.1f, Ui.MUTED))
            c.addView(r)
        }
        box.addView(c)
    }

    private fun chartCard(arr: JSONArray?) {
        val c = Ui.card(this)
        c.addView(TextView(this).apply {
            text = "📈 Динамика активности"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(12))
        })
        if (arr == null || arr.length() == 0) {
            c.addView(Ui.muted(this, "Нет данных", 30))
        } else {
            val chart = MiniChartView(this)
            val data = ArrayList<Triple<String, Float, Float>>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                data.add(Triple(o.optString("date").takeLast(5), o.optInt("imp").toFloat(), o.optInt("clk").toFloat()))
            }
            chart.data = data
            c.addView(chart, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(180)))
        }
        box.addView(c)
    }
}

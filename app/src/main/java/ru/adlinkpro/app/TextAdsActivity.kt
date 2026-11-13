package ru.adlinkpro.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

class TextAdsActivity : Activity() {

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
            val res = Api.post("ads.php", mapOf("action" to "textads_my"), token())
            runOnUiThread { render(res) }
        }.start()
    }

    private fun info(t: String) = TextView(this).apply {
        text = t; textSize = 12f; setTextColor(Ui.MUTED); setPadding(0, dp(2), 0, dp(2))
    }

    private fun render(res: JSONObject) {
        if (needLogin(this, res)) return
        box.removeAllViews()
        if (!res.optBoolean("ok")) {
            box.addView(TextView(this).apply { text = res.optString("error", "Ошибка сервера"); textSize = 14f; setTextColor(Ui.DANGER); setPadding(0, dp(8), 0, 0) })
            return
        }

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, dp(14)) }
        head.addView(TextView(this).apply {
            text = "📢 Мои текстовые объявления"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        head.addView(Ui.btnPrimary(this, "+ Разместить ссылку") {
            startActivity(Intent(this, TextAdCreateActivity::class.java))
        })
        box.addView(head)

        // Инфо-блок как на сайте
        box.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(Color.parseColor("#f0f9ff"))
                setStroke(dp(1), Color.parseColor("#7dd3fc"))
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
            addView(TextView(this@TextAdsActivity).apply {
                text = "💡 Как работает очередь: Новые объявления появляются сверху. Показывается 10 ссылок. Оплата разовая."
                textSize = 13f
                setTextColor(Color.parseColor("#0369a1"))
            })
        })

        val ads: JSONArray = res.optJSONArray("ads") ?: JSONArray()
        if (ads.length() == 0) {
            box.addView(Ui.card(this).apply { addView(Ui.muted(this@TextAdsActivity, "У вас пока нет объявлений", 24)) })
            return
        }

        for (i in 0 until ads.length()) {
            val a = ads.getJSONObject(i)
            val card = Ui.card(this)

            val titleRow = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, dp(6)) }
            titleRow.addView(TextView(this).apply {
                text = a.optString("text")
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                val c = a.optString("color")
                setTextColor(if (c.isNotBlank()) try { Color.parseColor(c) } catch (_: Exception) { Ui.TEXT } else Ui.TEXT)
                paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            })
            val badges = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, 0) }
            fun addBadge(t: String, col: Int) {
                badges.addView(TextView(this).apply {
                    text = t; textSize = 11f; setTextColor(col)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(if (col == Ui.SUCCESS) Color.parseColor("#d1fae5") else Color.parseColor("#dbeafe"))
                    }
                    setPadding(dp(8), dp(3), dp(8), dp(3))
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.setMargins(0, 0, dp(6), 0)
                    layoutParams = lp
                })
            }
            addBadge(when (a.optString("status")) {
                "active" -> "✅ Активно"
                "pending" -> "⏳ На модерации"
                "rejected" -> "❌ Отклонено"
                else -> "⏰ Истекло"
            }, when (a.optString("status")) {
                "active" -> Ui.SUCCESS
                "pending" -> Ui.WARNING
                "rejected" -> Ui.DANGER
                else -> Ui.MUTED
            })
            if (a.optString("color").isNotBlank()) addBadge("🎨 Цвет", Ui.PRIMARY)
            if (a.optBoolean("blink")) addBadge("✨ Моргает", Ui.PRIMARY)
            titleRow.addView(badges)
            card.addView(titleRow)

            card.addView(info("🔗 ${a.optString("link_url")}"))
            card.addView(info("📅 Размещено: ${a.optString("created")}"))
            card.addView(info("💰 Оплачено: ${a.optString("total_price")} (разовая оплата)"))
            card.addView(info("👁 Показов: ${a.optInt("impressions")} · 👆 Кликов: ${a.optInt("clicks")}"))

            val btnRow = LinearLayout(this).apply { setPadding(0, dp(10), 0, 0) }
            btnRow.addView(Ui.btnGhost(this, "✏️ Редактировать") {
                Toast.makeText(this, "Редактирование пока на сайте — добавим следующим обновлением", Toast.LENGTH_SHORT).show()
            })
            btnRow.addView(Ui.btnGhost(this, "🗑️ Удалить") {
                AlertDialog.Builder(this)
                    .setTitle("Удалить объявление?")
                    .setMessage("Остаток средств вернётся на рекламный счёт.")
                    .setPositiveButton("Удалить") { _, _ ->
                        Thread {
                            val r = Api.post("ads.php", mapOf("action" to "textad_delete", "id" to a.optInt("id").toString()), token())
                            runOnUiThread {
                                Toast.makeText(this, r.optString("message", if (r.optBoolean("ok")) "Удалено" else "Ошибка"), Toast.LENGTH_LONG).show()
                                if (r.optBoolean("ok")) load()
                            }
                        }.start()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }.apply {
                val lp = layoutParams as LinearLayout.LayoutParams
                lp.setMargins(dp(8), 0, 0, 0)
                layoutParams = lp
                setTextColor(Ui.DANGER)
            })
            card.addView(btnRow)
            box.addView(card)
        }
    }
}

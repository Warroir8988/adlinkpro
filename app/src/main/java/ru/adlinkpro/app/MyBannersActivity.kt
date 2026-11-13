package ru.adlinkpro.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class MyBannersActivity : Activity() {

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
            val res = Api.post("ads.php", mapOf("action" to "banners_my"), token())
            runOnUiThread { render(res) }
        }.start()
    }

    private fun loadImage(iv: ImageView, url: String) {
        Thread {
            try {
                val bmp = BitmapFactory.decodeStream(URL(url).openConnection().getInputStream())
                runOnUiThread { iv.setImageBitmap(bmp) }
            } catch (_: Exception) {}
        }.start()
    }

    private fun badge(text: String, color: Int): TextView = TextView(this).apply {
        this.text = text
        textSize = 11f
        setTextColor(color)
        background = GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(
                when (color) {
                    Ui.SUCCESS -> Color.parseColor("#d1fae5")
                    Ui.WARNING -> Color.parseColor("#fef3c7")
                    Ui.DANGER -> Color.parseColor("#fee2e2")
                    else -> Color.parseColor("#dbeafe")
                }
            )
        }
        setPadding(dp(8), dp(3), dp(8), dp(3))
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
            text = "🖼️ Мои баннеры"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        head.addView(Ui.btnPrimary(this, "+ Заказать баннер") {
            Toast.makeText(this, "Заказ баннера — в следующем обновлении", Toast.LENGTH_SHORT).show()
        })
        box.addView(head)

        val banners: JSONArray = res.optJSONArray("banners") ?: JSONArray()
        if (banners.length() == 0) {
            box.addView(Ui.card(this).apply { addView(Ui.muted(this@MyBannersActivity, "У вас пока нет баннеров", 24)) })
            return
        }

        for (i in 0 until banners.length()) {
            val b = banners.getJSONObject(i)
            val card = Ui.card(this)

            val iv = ImageView(this).apply {
                adjustViewBounds = true
                background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(Color.WHITE); setStroke(dp(1), Ui.BORDER) }
                setPadding(dp(2), dp(2), dp(2), dp(2))
            }
            card.addView(iv)
            loadImage(iv, b.optString("image_url"))

            val titleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(10), 0, dp(4)) }
            titleRow.addView(TextView(this).apply {
                text = b.optString("title")
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Ui.TEXT)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            titleRow.addView(badge(when (b.optString("status")) {
                "active" -> "✅ Активен"
                "pending" -> "⏳ На модерации"
                "rejected" -> "❌ Отклонён"
                else -> "⏰ Истёк"
            }, when (b.optString("status")) {
                "active" -> Ui.SUCCESS
                "pending" -> Ui.WARNING
                "rejected" -> Ui.DANGER
                else -> Ui.MUTED
            }))
            card.addView(titleRow)

            card.addView(info("🔗 ${b.optString("link_url")}"))
            card.addView(info("📅 Период: ${b.optString("start_date")} — ${b.optString("end_date")}"))
            card.addView(info("💰 Оплачено: ${b.optString("total_price")} (${b.optInt("days")} дн. × ${b.optString("price_per_day")})"))
            if (b.optString("status") == "active") card.addView(info("⏳ Осталось дней: ${b.optInt("days_left")}"))
            card.addView(info("👁 Показов: ${b.optInt("impressions")} · 👆 Кликов: ${b.optInt("clicks")}"))

            val btnRow = LinearLayout(this).apply { setPadding(0, dp(10), 0, 0) }
            btnRow.addView(Ui.btnGhost(this, "🗑️ Удалить") {
                AlertDialog.Builder(this)
                    .setTitle("Удалить баннер?")
                    .setMessage("Остаток средств вернётся на рекламный счёт.")
                    .setPositiveButton("Удалить") { _, _ ->
                        Thread {
                            val r = Api.post("ads.php", mapOf("action" to "banner_delete", "id" to b.optInt("id").toString()), token())
                            runOnUiThread {
                                Toast.makeText(this, r.optString("message", if (r.optBoolean("ok")) "Удалено" else "Ошибка"), Toast.LENGTH_LONG).show()
                                if (r.optBoolean("ok")) load()
                            }
                        }.start()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            })
            card.addView(btnRow)
            box.addView(card)
        }
    }
}

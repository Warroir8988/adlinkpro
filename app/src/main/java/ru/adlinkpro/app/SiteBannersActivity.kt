package ru.adlinkpro.app

import android.app.Activity
import android.content.Intent
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
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class SiteBannersActivity : Activity() {

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
            val res = Api.post("ads.php", mapOf("action" to "banners_active"), token())
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

    private fun render(res: JSONObject) {
        if (needLogin(this, res)) return
        box.removeAllViews()
        if (!res.optBoolean("ok")) {
            box.addView(TextView(this).apply { text = res.optString("error", "Ошибка сервера"); textSize = 14f; setTextColor(Ui.DANGER); setPadding(0, dp(8), 0, 0) })
            return
        }

        box.addView(TextView(this).apply {
            text = "🖼️ Баннерная реклама на сайте"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(14))
        })

        val promo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#4f46e5"), Color.parseColor("#7c3aed"))).apply { cornerRadius = dp(6).toFloat() }
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
        }
        promo.addView(TextView(this).apply { text = "📢"; textSize = 36f; setPadding(0, 0, dp(10), 0) })
        val promoMid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        promoMid.addView(TextView(this).apply {
            text = "Разместите свой баннер на всех страницах сайта!"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        promoMid.addView(TextView(this).apply {
            text = "Размер 468×60 px · Стоимость от 70,00 ₽/день · Ротация с другими рекламодателями · Статистика показов и кликов"
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(0, dp(4), 0, 0)
        })
        promo.addView(promoMid, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val orderBtn = TextView(this).apply {
            text = "+ Заказать\nбаннер"
            textSize = 13f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#4f46e5"))
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(Color.WHITE) }
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener { startActivity(Intent(this@SiteBannersActivity, BannerOrderActivity::class.java)) }
        }
        promo.addView(orderBtn)
        box.addView(promo)

        val banners: JSONArray = res.optJSONArray("banners") ?: JSONArray()
        box.addView(TextView(this).apply {
            text = "Активных баннеров: ${banners.length()}"
            textSize = 13f
            setTextColor(Ui.MUTED)
            setPadding(0, 0, 0, dp(10))
        })

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
            card.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(8), 0, 0)
                addView(TextView(this@SiteBannersActivity).apply {
                    text = "📅 Осталось: ${b.optInt("days_left")} дн."
                    textSize = 12f
                    setTextColor(Ui.PRIMARY)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(this@SiteBannersActivity).apply {
                    text = "👁 ${b.optInt("impressions")} показов"
                    textSize = 12f
                    setTextColor(Ui.MUTED)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(this@SiteBannersActivity).apply {
                    text = "👆 ${b.optInt("clicks")} кликов"
                    textSize = 12f
                    setTextColor(Ui.MUTED)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
            })
            box.addView(card)
        }
    }
}

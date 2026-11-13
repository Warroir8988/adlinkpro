package ru.adlinkpro.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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

class ReferralActivity : Activity() {

    private fun dp(v: Int) = Ui.dp(this, v)
    private fun token(): String = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""
    private lateinit var box: LinearLayout
    private var refLink = ""

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

    private fun copy(text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("adlinkpro", text))
        Toast.makeText(this, "✅ Скопировано!", Toast.LENGTH_SHORT).show()
    }

    private fun load() {
        Thread {
            val res = Api.post("referral.php", mapOf("action" to "info"), token())
            runOnUiThread { render(res) }
        }.start()
    }

    private fun addHeader() {
        box.addView(TextView(this).apply {
            text = "🎁 Реферальная программа"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(12))
        })
        box.addView(Ui.btnGhost(this, "🏆 Рейтинг рефереров") { startActivity(Intent(this, RatingActivity::class.java)) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) })
    }

    private fun render(res: JSONObject) {
        if (needLogin(this, res)) return
        box.removeAllViews()
        addHeader()
        if (!res.optBoolean("ok")) {
            box.addView(TextView(this).apply { text = res.optString("error", "Ошибка сервера"); textSize = 14f; setTextColor(Ui.DANGER); setPadding(0, dp(8), 0, 0) })
            return
        }

        refLink = "https://adlinkpro.ru/ref.php?code=" + res.optString("code", "")

        // Приветственный блок — как на сайте (градиент, голубая рамка)
        val welcome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#f0f9ff"), Color.parseColor("#e0f2fe"))).apply {
                cornerRadius = dp(6).toFloat()
                setStroke(dp(1), Color.parseColor("#7dd3fc"))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        welcome.addView(TextView(this).apply {
            text = "💰 Зарабатывайте на каждом приглашенном!"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0c4a6e"))
            setPadding(0, 0, 0, dp(10))
        })
        for (line in listOf(
            "✅ 2% от каждого пополнения счета реферала",
            "✅ 1% от его расходов на рекламу",
            "✅ 50,00 ₽ бонус рефералу при регистрации (только на рекламу)"
        )) welcome.addView(TextView(this).apply {
            text = line
            textSize = 13f
            setTextColor(Color.parseColor("#0c4a6e"))
            setPadding(0, dp(2), 0, dp(2))
        })
        box.addView(welcome)

        // Статистика: 4 плитки в две строки (на мобильном как на сайте)
        val st = res.optJSONObject("stats") ?: JSONObject()
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(14), 0, 0) }
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, 0) }
        fun tile(row: LinearLayout, value: String, label: String, color: Int) {
            row.addView(Ui.statTile(this, value, label, color),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(6), 0) })
        }
        tile(row1, st.optString("total", "0"), "Всего рефералов")
        tile(row1, st.optString("active", "0"), "Активных")
        tile(row2, st.optString("earned_total", "0,00 ₽"), "Всего заработано", Ui.SUCCESS)
        tile(row2, st.optString("earned_30d", "0,00 ₽"), "За 30 дней", Ui.PRIMARY)
        box.addView(row1)
        box.addView(row2)

        // Графики
        chartCard("📈 Начисления (30 дней)", res.optJSONArray("earnings_chart"), "amount",
            listOf(Color.parseColor("#10b981")), listOf("Начислено, ₽"))
        chartCard("👥 Новые рефералы (30 дней)", res.optJSONArray("refs_chart"), "count",
            listOf(Color.parseColor("#0ea5e9")), listOf("Рефералы"))

        // Реферальная ссылка
        val linkCard = Ui.card(this)
        linkCard.addView(Ui.title(this, "🔗 Ваша реферальная ссылка"))
        linkCard.addView(Ui.codeBox(this, refLink))
        linkCard.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
            fun addBtn(b: TextView) { addView(b, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dp(6), 0) }) }
            addBtn(Ui.btnPrimary(this@ReferralActivity, "📋 Копировать") { copy(refLink) })
            addBtn(Ui.btnGhost(this@ReferralActivity, "📤 Telegram") {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/share/url?url=" + Uri.encode(refLink) + "&text=" + Uri.encode("Попробуй AdLinkPro!"))))
            })
            addBtn(Ui.btnGhost(this@ReferralActivity, "💬 WhatsApp") {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=" + Uri.encode("AdLinkPro: $refLink"))))
            })
            addBtn(Ui.btnGhost(this@ReferralActivity, "🔵 VK") {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://vk.com/share.php?url=" + Uri.encode(refLink))))
            })
        })
        box.addView(linkCard)

        // Текстовые ссылки
        val txtCard = Ui.card(this)
        txtCard.addView(Ui.title(this, "📝 Текстовые ссылки"))
        val links = listOf(
            "Простая:" to "<a href=\"$refLink\">AdLinkPro — реклама и заработок</a>",
            "С описанием:" to "<a href=\"$refLink\">Размещай рекламу и зарабатывай на AdLinkPro!</a>",
            "С бонусом:" to "<a href=\"$refLink\">🔥 Регистрируйся в AdLinkPro и получи бонус 50,00 ₽!</a>"
        )
        for ((label, code) in links) {
            txtCard.addView(TextView(this).apply { text = label; textSize = 12f; setTextColor(Ui.MUTED); setPadding(0, dp(4), 0, dp(4)) })
            txtCard.addView(Ui.codeBox(this, code))
            val b = Ui.btnGhost(this, "📋 Копировать") { copy(code) }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(6), 0, dp(10))
            txtCard.addView(b, lp)
        }
        box.addView(txtCard)

        // Готовые баннеры
        val banCard = Ui.card(this)
        banCard.addView(Ui.title(this, "🖼️ Готовые баннеры"))
        banCard.addView(TextView(this).apply {
            text = "Скопируйте HTML-код и вставьте на свой сайт. Под каждым баннером — предварительный просмотр."
            textSize = 12f
            setTextColor(Ui.MUTED)
            setPadding(0, 0, 0, dp(10))
        })
        val bannerStatic = "<a href=\"$refLink\" target=\"_blank\"><img src=\"https://adlinkpro.ru/assets/banners/banner_468x60.png\" alt=\"AdLinkPro\" width=\"468\" height=\"60\" border=\"0\"></a>"
        val bannerAnim = "<script src=\"https://adlinkpro.ru/assets/banners/alp468x60.js?ref=" + res.optString("code", "") + "\"></script>"
        banCard.addView(TextView(this).apply { text = "Баннер 468×60"; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Ui.TEXT); setPadding(0, 0, 0, dp(6)) })
        banCard.addView(Ui.codeBox(this, bannerStatic))
        banCard.addView(Ui.btnGhost(this, "📋 Копировать") { copy(bannerStatic) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, 0) })
        banCard.addView(TextView(this).apply { text = "Как будет выглядеть баннер:"; textSize = 12f; setTextColor(Ui.MUTED); setPadding(0, dp(10), 0, dp(6)) })
        val preview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { cornerRadius = dp(8).toFloat(); setColor(Ui.CARD); setStroke(dp(1), Ui.BORDER) }
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val iv = ImageView(this).apply { adjustViewBounds = true }
        preview.addView(iv)
        banCard.addView(preview)
        Thread {
            try {
                val bmp = BitmapFactory.decodeStream(URL("https://adlinkpro.ru/assets/banners/banner_468x60.png").openConnection().getInputStream())
                runOnUiThread { iv.setImageBitmap(bmp) }
            } catch (_: Exception) {}
        }.start()
        banCard.addView(TextView(this).apply { text = "Баннер 468×60 — анимированный"; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Ui.TEXT); setPadding(0, dp(14), 0, dp(6)) })
        banCard.addView(Ui.codeBox(this, bannerAnim))
        banCard.addView(Ui.btnGhost(this, "📋 Копировать") { copy(bannerAnim) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, 0) })
        box.addView(banCard)

        // Мои рефералы
        val refs = res.optJSONArray("referrals") ?: JSONArray()
        val refCard = Ui.card(this)
        refCard.addView(Ui.title(this, "👥 Мои рефералы (${refs.length()})"))
        if (refs.length() == 0) {
            refCard.addView(Ui.muted(this, "Пока никто не зарегистрировался по вашей ссылке"))
        } else {
            for (i in 0 until refs.length()) {
                val r = refs.getJSONObject(i)
                refCard.addView(TextView(this).apply {
                    text = "${r.optString("email")} · с ${r.optString("date")} · заработано ${r.optString("earned", "0,00 ₽")}"
                    textSize = 13f
                    setTextColor(Ui.TEXT)
                    setPadding(0, dp(4), 0, dp(4))
                })
            }
        }
        box.addView(refCard)

        // История начислений
        val hist = res.optJSONArray("history") ?: JSONArray()
        val histCard = Ui.card(this)
        histCard.addView(Ui.title(this, "💸 История начислений"))
        if (hist.length() == 0) {
            histCard.addView(Ui.muted(this, "Начислений пока не было"))
        } else {
            for (i in 0 until hist.length()) {
                val h = hist.getJSONObject(i)
                histCard.addView(TextView(this).apply {
                    text = "${h.optString("date")} · +${h.optString("amount")} ₽ · от ${h.optString("from")}"
                    textSize = 13f
                    setTextColor(Ui.TEXT)
                    setPadding(0, dp(4), 0, dp(4))
                })
            }
        }
        box.addView(histCard)
    }

    private fun chartCard(title: String, arr: JSONArray?, key: String, colors: List<Int>, names: List<String>) {
        val c = Ui.card(this)
        c.addView(Ui.title(this, title, 14f))
        if (arr == null || arr.length() == 0) {
            c.addView(Ui.muted(this, "Нет данных"))
        } else {
            val chart = MiniChartView(this)
            val data = ArrayList<Triple<String, Float, Float>>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                data.add(Triple(o.optString("date").takeLast(5), o.optDouble(key, 0.0).toFloat(), 0f))
            }
            chart.data = data
            chart.seriesColors = colors
            chart.seriesNames = names
            c.addView(chart, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(150)))
        }
        box.addView(c)
    }
}

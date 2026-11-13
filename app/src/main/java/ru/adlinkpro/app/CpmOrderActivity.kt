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
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject

class CpmOrderActivity : Activity() {

    private fun dp(v: Int) = Ui.dp(this, v)
    private fun token(): String = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""

    private var imp = 1000
    private var impLabel = "1 000 показов — 29,40 руб."
    private var impLabels: List<Pair<Int, String>> = emptyList()
    private lateinit var impField: TextView
    private lateinit var payNote: TextView
    private lateinit var status: TextView
    private lateinit var send: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.CARD
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.BG) }
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(24)) }
        scroll.addView(box)
        root.addView(scroll)
        setContentView(root)

        box.addView(TextView(this).apply {
            text = "📦 Тираж — гарантированные показы"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(8))
        })
        box.addView(TextView(this).apply {
            text = "Пакет показов по цене 29,40 руб. за 1000. После модерации баннер крутится на витрине, каждый показ засчитывается."
            textSize = 13f
            setTextColor(Ui.MUTED)
            setPadding(0, 0, 0, dp(4))
        })
        box.addView(TextView(this).apply {
            text = "✅ Повторный заказ: скидка 2% уже учтена в цене"
            textSize = 14f
            setTextColor(Ui.SUCCESS)
            setPadding(0, 0, 0, dp(14))
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#f8fafc"))
                setStroke(dp(1), Ui.BORDER)
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        box.addView(card)

        card.addView(TextView(this).apply {
            text = "Новый заказ"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(10))
        })

        fun fieldBg() = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(Ui.CARD); setStroke(dp(1), Ui.BORDER) }
        fun label(t: String) = TextView(this).apply {
            text = t; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Ui.TEXT); setPadding(0, dp(10), 0, dp(4))
        }

        card.addView(label("Название заказа"))
        val name = EditText(this).apply {
            hint = "Например: Мой баннер"
            textSize = 14f; setTextColor(Ui.TEXT); setHintTextColor(Ui.MUTED)
            background = fieldBg(); setPadding(dp(12), dp(10), dp(12), dp(10))
            filters = arrayOf(android.text.InputFilter.LengthFilter(100))
        }
        card.addView(name)

        card.addView(label("Пакет показов"))
        impField = TextView(this).apply {
            text = impLabel
            textSize = 14f; setTextColor(Ui.TEXT)
            background = fieldBg(); setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        impField.setOnClickListener {
            if (impLabels.isEmpty()) return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("Пакет показов")
                .setSingleChoiceItems(impLabels.map { it.second }.toTypedArray(),
                    impLabels.indexOfFirst { it.first == imp }.coerceAtLeast(0)) { d, which ->
                    imp = impLabels[which].first
                    impLabel = impLabels[which].second
                    impField.text = impLabel
                    d.dismiss()
                }.show()
        }
        card.addView(impField)

        card.addView(label("Ссылка, куда ведёт баннер (обязательно, если загружаете файл)"))
        val url = EditText(this).apply {
            hint = "https://yoursite.com"
            textSize = 14f; setTextColor(Ui.TEXT); setHintTextColor(Ui.MUTED)
            background = fieldBg(); setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        card.addView(url)

        card.addView(label("Рекламный код (HTML баннера/ссылки)"))
        val html = EditText(this).apply {
            hint = "Вставьте HTML баннера или ссылки"
            textSize = 14f; minLines = 6; gravity = Gravity.TOP
            setTextColor(Ui.TEXT); setHintTextColor(Ui.MUTED)
            background = fieldBg(); setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        card.addView(html)

        card.addView(TextView(this).apply {
            text = "⚠️ Загрузка файла баннера с телефона пока недоступна — вставьте HTML-код (как на сайте, вариант без файла). Файл добавим в следующем обновлении."
            textSize = 12f; setTextColor(Ui.WARNING); setPadding(0, dp(8), 0, 0)
        })

        payNote = TextView(this).apply {
            textSize = 13f; setTextColor(Ui.MUTED); setPadding(0, dp(10), 0, 0)
        }
        card.addView(payNote)

        status = TextView(this).apply { setTextColor(Ui.DANGER); textSize = 13f; setPadding(0, dp(10), 0, 0) }
        card.addView(status)

        send = TextView(this).apply {
            text = "🛒 Оформить заказ"
            textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(Ui.PRIMARY) }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(14), 0, 0)
            layoutParams = lp
        }
        card.addView(send)

        // Балансы + пакеты
        Thread {
            val info = Api.post("ads.php", mapOf("action" to "info"), token())
            runOnUiThread {
                if (info.optBoolean("ok")) {
                    payNote.text = "Рекламный баланс: ${info.optString("ad_balance")} · Основной: ${info.optString("balance")}\nОплата: сначала рекламный баланс, остаток — с основного."
                    val pk = info.optJSONArray("packages")
                    if (pk != null) {
                        val list = ArrayList<Pair<Int, String>>()
                        for (i in 0 until pk.length()) {
                            val o = pk.getJSONObject(i)
                            list.add(o.optInt("imp") to o.optString("label"))
                        }
                        impLabels = list
                    }
                }
            }
        }.start()

        send.setOnClickListener {
            val nm = name.text.toString().trim()
            val code = html.text.toString().trim()
            if (nm.length < 3) { status.text = "Название слишком короткое"; return@setOnClickListener }
            if (code.isEmpty()) { status.text = "Вставьте HTML баннера или ссылки"; return@setOnClickListener }

            send.isEnabled = false
            status.text = ""
            Thread {
                val res = Api.post("ads.php", mapOf(
                    "action" to "cpm_order",
                    "name" to nm,
                    "impressions" to imp.toString(),
                    "banner_url" to url.text.toString().trim(),
                    "html_code" to code
                ), token())
                runOnUiThread {
                    send.isEnabled = true
                    if (needLogin(this, res)) return@runOnUiThread
                    if (res.optBoolean("ok")) {
                        Toast.makeText(this, res.optString("message", "Заказ создан"), Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        status.text = res.optString("error", "Ошибка")
                    }
                }
            }.start()
        }
    }
}

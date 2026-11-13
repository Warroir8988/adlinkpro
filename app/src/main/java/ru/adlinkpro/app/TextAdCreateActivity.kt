package ru.adlinkpro.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

class TextAdCreateActivity : Activity() {

    private fun dp(v: Int) = Ui.dp(this, v)
    private fun token(): String = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""

    private val palette = listOf("#ff0000", "#0000ff", "#059669", "#f59e0b", "#7c3aed", "#0ea5e9", "#ec4899", "#1e293b")
    private var selectedColor = "#3498db"

    private lateinit var counter: TextView
    private lateinit var previewText: TextView
    private lateinit var priceCalc: TextView
    private lateinit var balanceNote: TextView
    private lateinit var status: TextView
    private lateinit var send: TextView
    private lateinit var useColor: CheckBox
    private lateinit var blinkCheck: CheckBox
    private lateinit var adText: EditText

    private var balance = 0.0
    private var blinkEnabled = false
    private var blinkOn = true
    private val blinkHandler = Handler(Looper.getMainLooper())

    private val blinkRunnable = object : Runnable {
        override fun run() {
            blinkOn = !blinkOn
            previewText.alpha = if (blinkOn) 1f else 0.3f
            if (blinkEnabled) blinkHandler.postDelayed(this, 500)
        }
    }

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
            text = "📢 Заказать текстовую рекламу"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(14))
        })

        // Тарифы — голубой градиент как на сайте
        val tariffs = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#f0f9ff"), Color.parseColor("#e0f2fe"))).apply {
                cornerRadius = dp(6).toFloat()
                setStroke(dp(2), Color.parseColor("#7dd3fc"))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
        }
        tariffs.addView(TextView(this).apply {
            text = "💡 Тарифы (разовая оплата)"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0c4a6e"))
            setPadding(0, 0, 0, dp(8))
        })
        for (line in listOf(
            "📝 Базовый: 5,00 ₽ — обычная ссылка",
            "🎨 Цветной: 7,00 ₽ — текст выбранного цвета (+2,00 ₽)",
            "✨ Моргающий: 8,00 ₽ — мигающий текст (+3,00 ₽)",
            "🌈 Всё сразу: 10,00 ₽ — цвет + моргалка"
        )) tariffs.addView(TextView(this).apply {
            text = line; textSize = 13f; setTextColor(Color.parseColor("#0369a1")); setPadding(0, dp(2), 0, dp(2))
        })
        tariffs.addView(TextView(this).apply {
            text = "💡 Как работает: Вы платите один раз, и ваша ссылка появляется в очереди. Новые объявления — сверху, старые опускаются ниже. Показывается 10 ссылок."
            textSize = 12f
            setTextColor(Color.parseColor("#0369a1"))
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(Color.WHITE) }
            setPadding(dp(10), dp(8), dp(10), dp(8))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(10), 0, 0)
            layoutParams = lp
        })
        box.addView(tariffs)

        val card = Ui.card(this)

        fun fieldBg() = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(Ui.BG); setStroke(dp(1), Ui.BORDER) }
        fun label(t: String) = TextView(this).apply {
            text = t; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Ui.TEXT); setPadding(0, dp(10), 0, dp(4))
        }

        card.addView(label("Текст ссылки (макс. 40 символов)"))
        adText = EditText(this).apply {
            hint = "Например: Лучший хостинг!"
            textSize = 14f; setTextColor(Ui.TEXT); setHintTextColor(Ui.MUTED)
            background = fieldBg(); setPadding(dp(12), dp(10), dp(12), dp(10))
            filters = arrayOf(android.text.InputFilter.LengthFilter(40))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    counter.text = "${s?.length ?: 0} / 40"
                    updatePreview()
                }
            })
        }
        card.addView(adText)
        counter = TextView(this).apply { text = "0 / 40"; textSize = 11f; setTextColor(Ui.MUTED); gravity = Gravity.END; setPadding(0, dp(2), 0, 0) }
        card.addView(counter)

        card.addView(label("Ссылка (куда ведёт)"))
        val link = EditText(this).apply {
            hint = "https://yoursite.com"
            textSize = 14f; setTextColor(Ui.TEXT); setHintTextColor(Ui.MUTED)
            background = fieldBg(); setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        card.addView(link)

        card.addView(label("🎨 Цвет текста (необязательно, +2,00 ₽)"))
        useColor = CheckBox(this).apply {
            text = "Применить цвет"
            textSize = 13f
            setTextColor(Ui.TEXT)
            setOnCheckedChangeListener { _, _ -> updatePreview(); updatePrice() }
        }
        card.addView(useColor)
        val swatches = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, dp(4)) }
        for (hex in palette) {
            val sw = TextView(this).apply {
                val g = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(hex))
                    setStroke(dp(2), Color.TRANSPARENT)
                }
                background = g
                setOnClickListener {
                    selectedColor = hex
                    for (j in 0 until swatches.childCount) {
                        (swatches.getChildAt(j) as TextView).background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor(palette[j]))
                            setStroke(dp(2), if (palette[j] == hex) Ui.TEXT else Color.TRANSPARENT)
                        }
                    }
                    updatePreview()
                }
                val lp = LinearLayout.LayoutParams(dp(34), dp(34))
                lp.setMargins(0, 0, dp(10), 0)
                layoutParams = lp
            }
            swatches.addView(sw)
        }
        card.addView(swatches)

        card.addView(label("✨ Моргающая ссылка (+3,00 ₽)"))
        blinkCheck = CheckBox(this).apply {
            text = "Сделать текст мигающим"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(Color.parseColor("#f8fafc")) }
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnCheckedChangeListener { _, checked ->
                blinkEnabled = checked
                blinkHandler.removeCallbacks(blinkRunnable)
                if (checked) blinkHandler.post(blinkRunnable) else previewText.alpha = 1f
                updatePrice()
            }
        }
        card.addView(blinkCheck)

        card.addView(label("👁 Предпросмотр:"))
        val previewBox = LinearLayout(this).apply {
            background = fieldBg()
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        previewText = TextView(this).apply {
            text = "Ваш текст ссылки"
            textSize = 15f
            setTextColor(Ui.TEXT)
            paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        }
        previewBox.addView(previewText)
        card.addView(previewBox)

        priceCalc = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#0369a1"))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#f0f9ff"))
                setStroke(dp(1), Color.parseColor("#7dd3fc"))
            }
            setPadding(dp(12), dp(10), dp(12), dp(10))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(14), 0, 0)
            layoutParams = lp
        }
        card.addView(priceCalc)

        balanceNote = TextView(this).apply { textSize = 12f; setTextColor(Ui.MUTED); setPadding(0, dp(10), 0, 0) }
        card.addView(balanceNote)

        status = TextView(this).apply { setTextColor(Ui.DANGER); textSize = 13f; setPadding(0, dp(10), 0, 0) }
        card.addView(status)

        // Жёлтый блок согласия
        val agreeCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor("#fef3c7"))
                setStroke(dp(1), Color.parseColor("#fde68a"))
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(14), 0, 0) }
        }
        val agree = CheckBox(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#78350f"))
            text = "Я согласен с условиями размещения рекламы и подтверждаю, что ссылка не ведёт на запрещённые ресурсы."
        }
        agreeCard.addView(agree)
        card.addView(agreeCard)

        send = TextView(this).apply {
            text = "💳 Оплатить и отправить на модерацию"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(Ui.PRIMARY) }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(14), 0, 0)
            layoutParams = lp
        }
        card.addView(send)
        box.addView(card)

        Thread {
            val info = Api.post("ads.php", mapOf("action" to "info"), token())
            runOnUiThread {
                if (info.optBoolean("ok")) {
                    balanceNote.text = "Ваш баланс: ${info.optString("balance")} · Рекламный: ${info.optString("ad_balance")}"
                    balance = parseMoney(info.optString("ad_balance")) + parseMoney(info.optString("balance"))
                    updatePrice()
                }
            }
        }.start()

        updatePreview()
        updatePrice()

        send.setOnClickListener {
            send.isEnabled = false
            status.text = ""
            Thread {
                val res = Api.post("ads.php", mapOf(
                    "action" to "textad_add",
                    "text" to adText.text.toString().trim(),
                    "link_url" to link.text.toString().trim(),
                    "use_color" to if (useColor.isChecked) "1" else "0",
                    "color" to selectedColor,
                    "blink" to if (blinkCheck.isChecked) "1" else "0",
                    "agree" to if (agree.isChecked) "1" else "0"
                ), token())
                runOnUiThread {
                    send.isEnabled = true
                    if (needLogin(this, res)) return@runOnUiThread
                    if (res.optBoolean("ok")) {
                        Toast.makeText(this, res.optString("message", "Отправлено на модерацию"), Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        status.text = res.optString("error", "Ошибка")
                    }
                }
            }.start()
        }
    }

    private fun parseMoney(s: String): Double {
        val clean = s.replace("₽", "").replace(" ", "").replace(',', '.').trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun updatePreview() {
        val t = adText.text.toString().trim().ifEmpty { "Ваш текст ссылки" }
        previewText.text = t
        previewText.setTextColor(if (useColor.isChecked) Color.parseColor(selectedColor) else Ui.TEXT)
    }

    private fun updatePrice() {
        var total = 5.0
        val opts = ArrayList<String>()
        if (useColor.isChecked) { total += 2.0; opts.add("цвет +2,00₽") }
        if (blinkCheck.isChecked) { total += 3.0; opts.add("моргалка +3,00₽") }
        val sb = StringBuilder()
        sb.append("Базовая цена: 5,00 ₽\n")
        if (opts.isNotEmpty()) sb.append("Опции: ${opts.joinToString(", ")}\n")
        sb.append("Итого к оплате: ${String.format(java.util.Locale.US, "%.2f", total).replace('.', ',')} ₽ (разовая оплата)")
        if (total > balance) sb.append("\n⚠️ Недостаточно средств")
        priceCalc.text = sb.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        blinkHandler.removeCallbacks(blinkRunnable)
    }
}

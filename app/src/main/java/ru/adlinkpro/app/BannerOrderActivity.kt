package ru.adlinkpro.app

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

class BannerOrderActivity : Activity() {

    private fun dp(v: Int) = Ui.dp(this, v)
    private fun token(): String = getSharedPreferences("app", MODE_PRIVATE).getString("token", "") ?: ""

    private var fileBytes: ByteArray? = null
    private var fileName: String? = null
    private var fileMime: String? = null

    private lateinit var preview: ImageView
    private lateinit var previewEmpty: TextView
    private lateinit var fileBtn: TextView
    private lateinit var fileNote: TextView
    private lateinit var imageUrl: EditText
    private lateinit var days: EditText
    private lateinit var priceCalc: TextView
    private lateinit var balanceNote: TextView
    private lateinit var agree: CheckBox
    private lateinit var status: TextView
    private lateinit var send: TextView

    private var balance = 0.0

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
            text = "🖼️ Заказать баннерную рекламу"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Ui.TEXT)
            setPadding(0, 0, 0, dp(14))
        })

        val how = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#f0f9ff"), Color.parseColor("#e0f2fe"))).apply {
                cornerRadius = dp(6).toFloat()
                setStroke(dp(2), Color.parseColor("#7dd3fc"))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(14)) }
        }
        how.addView(TextView(this).apply {
            text = "📋 Как работает баннерная реклама"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0c4a6e"))
            setPadding(0, 0, 0, dp(8))
        })
        for (line in listOf(
            "📐 Размер баннера: 468×60 px",
            "💰 Стоимость: 70,00 ₽ за 1 день показа",
            "🔄 Ваш баннер участвует в ротации с другими рекламодателями",
            "🛡️ Все баннеры проходят модерацию перед публикацией",
            "📊 Отслеживайте показы и клики в личном кабинете",
            "💳 Оплата списывается с баланса сразу на весь срок",
            "📍 Показывается в шапке сайта и на специальной странице"
        )) how.addView(TextView(this).apply {
            text = line; textSize = 13f; setTextColor(Color.parseColor("#0369a1")); setPadding(0, dp(2), 0, dp(2))
        })
        box.addView(how)

        val card = Ui.card(this)
        fun fieldBg() = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(Ui.BG); setStroke(dp(1), Ui.BORDER) }
        fun label(t: String) = TextView(this).apply {
            text = t; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Ui.TEXT); setPadding(0, dp(10), 0, dp(4))
        }

        card.addView(label("Название баннера"))
        val title = EditText(this).apply {
            hint = "Например: Мой интернет-магазин"
            textSize = 14f; setTextColor(Ui.TEXT); setHintTextColor(Ui.MUTED)
            background = fieldBg(); setPadding(dp(12), dp(10), dp(12), dp(10))
            filters = arrayOf(android.text.InputFilter.LengthFilter(255))
        }
        card.addView(title)

        card.addView(label("Ссылка (куда ведёт баннер)"))
        val link = EditText(this).apply {
            hint = "https://yoursite.com"
            textSize = 14f; setTextColor(Ui.TEXT); setHintTextColor(Ui.MUTED)
            background = fieldBg(); setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        card.addView(link)

        card.addView(label("URL изображения баннера (468×60 px)"))
        imageUrl = EditText(this).apply {
            hint = "https://yoursite.com/banner.jpg"
            textSize = 14f; setTextColor(Ui.TEXT); setHintTextColor(Ui.MUTED)
            background = fieldBg(); setPadding(dp(12), dp(10), dp(12), dp(10))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!s.isNullOrBlank()) {
                        fileBytes = null
                        fileNote.text = ""
                        loadUrlPreview(s.toString().trim())
                    } else {
                        preview.setImageBitmap(null)
                        preview.visibility = android.view.View.GONE
                        previewEmpty.visibility = android.view.View.VISIBLE
                    }
                }
            })
        }
        card.addView(imageUrl)
        card.addView(TextView(this).apply {
            text = "Выберите файл с телефона (рекомендуется — картинка сохранится на нашем сервере) или вставьте ссылку"
            textSize = 11f
            setTextColor(Ui.MUTED)
            setPadding(0, dp(2), 0, dp(8))
        })

        card.addView(label("Файл изображения (PNG, JPEG, GIF, WebP — до 2 МБ)"))
        fileBtn = Ui.btnGhost(this, "📂 Выбрать файл с телефона") {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(Intent.createChooser(intent, "Выбрать баннер"), 101)
        }
        card.addView(fileBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(6)) })
        fileNote = TextView(this).apply { textSize = 12f; setTextColor(Ui.SUCCESS); setPadding(0, 0, 0, dp(6)) }
        card.addView(fileNote)

        card.addView(label("Предпросмотр (468×60):"))
        val previewBoxLocal = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(Color.parseColor("#f8fafc")); setStroke(dp(2), Ui.BORDER) }
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        preview = ImageView(this).apply { adjustViewBounds = true; visibility = android.view.View.GONE }
        previewEmpty = TextView(this).apply { text = "Предпросмотр появится здесь"; textSize = 12f; setTextColor(Ui.MUTED) }
        previewBoxLocal.addView(preview)
        previewBoxLocal.addView(previewEmpty)
        card.addView(previewBoxLocal)

        card.addView(label("Количество дней показа"))
        days = EditText(this).apply {
            hint = "1"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("1")
            textSize = 14f; setTextColor(Ui.TEXT); setHintTextColor(Ui.MUTED)
            background = fieldBg(); setPadding(dp(12), dp(10), dp(12), dp(10))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { updatePrice() }
            })
        }
        card.addView(days)

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
        agree = CheckBox(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#78350f"))
            text = "Я согласен с условиями размещения рекламы и подтверждаю, что баннер не содержит запрещённого контента."
        }
        agreeCard.addView(agree)
        card.addView(agreeCard)

        status = TextView(this).apply { setTextColor(Ui.DANGER); textSize = 13f; setPadding(0, dp(10), 0, 0) }
        card.addView(status)

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
                    balance = parseMoney(info.optString("balance")) + parseMoney(info.optString("ad_balance"))
                    updatePrice()
                }
            }
        }.start()

        send.setOnClickListener {
            val t = title.text.toString().trim()
            val l = link.text.toString().trim()
            val d = days.text.toString().toIntOrNull() ?: 0
            val img = imageUrl.text.toString().trim()

            if (t.length < 3) { status.text = "Введите название баннера"; return@setOnClickListener }
            if (!l.startsWith("http")) { status.text = "Укажите корректную ссылку (https://...)"; return@setOnClickListener }
            if (d < 1 || d > 365) { status.text = "Количество дней: от 1 до 365"; return@setOnClickListener }
            val hasFile = fileBytes != null
            if (!hasFile && img.isEmpty()) { status.text = "Выберите файл или укажите URL изображения баннера"; return@setOnClickListener }
            if (!agree.isChecked) { status.text = "Подтвердите согласие с условиями"; return@setOnClickListener }

            send.isEnabled = false
            status.text = ""
            Thread {
                val res: JSONObject
                val fields = mapOf(
                    "action" to "banner_order",
                    "title" to t,
                    "link_url" to l,
                    "days" to d.toString(),
                    "image_url" to (if (hasFile) "" else img),
                    "agree" to if (agree.isChecked) "1" else "0"
                )
                res = if (hasFile) {
                    ApiUpload.post(this, "ads.php", fields, "image_file", fileName, fileMime, fileBytes)
                } else {
                    Api.post("ads.php", fields, token())
                }
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

        updatePrice()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
                if (bytes.size > 2 * 1024 * 1024) {
                    status.text = "Файл больше 2 МБ"
                    return
                }
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp == null) {
                    status.text = "Это не изображение"
                    return
                }
                fileBytes = bytes
                imageUrl.setText("")
                fileMime = contentResolver.getType(uri) ?: "image/png"
                fileName = queryName(uri) ?: "banner.${fileMime?.substringAfter('/') ?: "png"}"
                fileNote.text = "✅ Выбран файл: $fileName (${bytes.size / 1024} КБ)"
                preview.setImageBitmap(bmp)
                preview.visibility = android.view.View.VISIBLE
                previewEmpty.visibility = android.view.View.GONE
                status.text = ""
            } catch (e: Exception) {
                status.text = "Не удалось прочитать файл"
            }
        }
    }

    private fun queryName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        } catch (_: Exception) { null }
    }

    private fun loadUrlPreview(url: String) {
        Thread {
            try {
                val bmp = BitmapFactory.decodeStream(java.net.URL(url).openConnection().getInputStream())
                runOnUiThread {
                    if (bmp != null) {
                        preview.setImageBitmap(bmp)
                        preview.visibility = android.view.View.VISIBLE
                        previewEmpty.visibility = android.view.View.GONE
                    } else {
                        previewEmpty.text = "❌ Не удалось загрузить изображение"
                        previewEmpty.visibility = android.view.View.VISIBLE
                        preview.visibility = android.view.View.GONE
                    }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun parseMoney(s: String): Double {
        val clean = s.replace("₽", "").replace(" ", "").replace(',', '.').trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun updatePrice() {
        val d = days.text.toString().toIntOrNull() ?: 0
        val total = d * 70.0
        val sb = StringBuilder()
        sb.append("Цена за день: 70,00 ₽\n")
        sb.append("Дней: $d\n")
        sb.append("Итого к оплате: ${String.format(java.util.Locale.US, "%.2f", total).replace('.', ',')} ₽")
        if (d > 0 && total > balance) sb.append("\n⚠️ Недостаточно средств на балансе")
        priceCalc.text = sb.toString()
    }
}

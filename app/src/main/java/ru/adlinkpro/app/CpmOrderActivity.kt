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
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
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

    private var fileBytes: ByteArray? = null
    private var fileName: String? = null
    private var fileMime: String? = null

    private lateinit var impField: TextView
    private lateinit var payNote: TextView
    private lateinit var status: TextView
    private lateinit var send: TextView
    private lateinit var fileBtn: TextView
    private lateinit var fileNote: TextView
    private lateinit var preview: ImageView
    private lateinit var previewEmpty: TextView
    private lateinit var bannerUrl: EditText

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

        card.addView(label("Файл баннера — PNG/JPEG/GIF/WebP, ровно 468×60, до 2 МБ"))
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

        val previewBox = LinearLayout(this).apply {
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(Color.WHITE); setStroke(dp(2), Ui.BORDER) }
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(6), dp(6), dp(6))
            visibility = android.view.View.GONE
        }
        preview = ImageView(this).apply { adjustViewBounds = true }
        previewEmpty = TextView(this).apply { text = "Предпросмотр появится здесь"; textSize = 12f; setTextColor(Ui.MUTED) }
        previewBox.addView(preview)
        previewBox.addView(previewEmpty)
        card.addView(previewBox)
        card.tag = previewBox

        card.addView(label("Ссылка, куда ведёт баннер (обязательно, если загружаете файл)"))
        bannerUrl = EditText(this).apply {
            hint = "https://yoursite.com"
            textSize = 14f; setTextColor(Ui.TEXT); setHintTextColor(Ui.MUTED)
            background = fieldBg(); setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        card.addView(bannerUrl)

        card.addView(label("Рекламный код (HTML баннера/ссылки) — если без файла"))
        val html = EditText(this).apply {
            hint = "Вставьте HTML баннера или ссылки"
            textSize = 14f; minLines = 6; gravity = Gravity.TOP
            setTextColor(Ui.TEXT); setHintTextColor(Ui.MUTED)
            background = fieldBg(); setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        card.addView(html)

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
            val hasFile = fileBytes != null

            if (nm.length < 3) { status.text = "Название слишком короткое"; return@setOnClickListener }
            if (!hasFile && code.isEmpty()) { status.text = "Выберите файл баннера или вставьте HTML-код"; return@setOnClickListener }
            if (hasFile && !bannerUrl.text.toString().trim().startsWith("http")) {
                status.text = "Укажите ссылку, куда ведёт баннер"; return@setOnClickListener
            }

            send.isEnabled = false
            status.text = ""
            Thread {
                val res: JSONObject
                val fields = mapOf(
                    "action" to "cpm_order",
                    "name" to nm,
                    "impressions" to imp.toString(),
                    "banner_url" to bannerUrl.text.toString().trim(),
                    "html_code" to code
                )
                res = if (hasFile) {
                    ApiUpload.post(this, "ads.php", fields, "banner_file", fileName, fileMime, fileBytes)
                } else {
                    Api.post("ads.php", fields, token())
                }
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
                fileMime = contentResolver.getType(uri) ?: "image/png"
                fileName = queryName(uri) ?: "banner.${fileMime?.substringAfter('/') ?: "png"}"
                fileNote.text = "✅ Выбран файл: $fileName (${bytes.size / 1024} КБ)"
                preview.setImageBitmap(bmp)
                preview.visibility = android.view.View.VISIBLE
                previewEmpty.visibility = android.view.View.GONE
                (card.tag as? android.view.View)?.visibility = android.view.View.VISIBLE
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
}

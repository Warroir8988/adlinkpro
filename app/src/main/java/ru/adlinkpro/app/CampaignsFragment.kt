package ru.adlinkpro.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class CampaignsFragment : Fragment() {
    // Категории — точная копия вебовского списка (campaign_add.php), код + название
    private data class CatItem(val code: String, val name: String)
    private val catItems = listOf(
        CatItem("earn", "Инвестиции, заработок и фермы"),
        CatItem("promo", "Реклама и партнерские программы"),
        CatItem("faucets", "Раздача бонусов (краны)"),
        CatItem("info", "Информационные сайты и сервисы"),
        CatItem("auto", "Авто, мото и другой транспорт"),
        CatItem("nature", "Животные и растения"),
        CatItem("games", "Игры для компьютеров и консолей"),
        CatItem("health", "Медицина, фитнес и здоровье"),
        CatItem("gadgets", "Мобильные телефоны и гаджеты"),
        CatItem("movies", "Музыка, искусство и кино"),
        CatItem("tech", "Наука и техника"),
        CatItem("realestate", "Недвижимость и строительство"),
        CatItem("news", "Новости и СМИ"),
        CatItem("travel", "Путешествия и туризм"),
        CatItem("files", "Софт, операционные системы и торренты"),
        CatItem("social", "Социальные сети, форумы и порталы"),
        CatItem("sport", "Спорт и ставки"),
        CatItem("business", "Товары, услуги и бизнес"),
        CatItem("seo", "SEO и создание сайтов"),
        CatItem("shop", "Интернет-магазины"),
        CatItem("crypto", "Крипта"),
        CatItem("casino", "Казино / лотереи"),
        CatItem("dating", "Дейтинг / знакомства"),
        CatItem("finance", "Финансы / кредиты"),
        CatItem("other", "Прочее")
    )
    private val selectedCats = mutableSetOf<String>()

    private var savedToken: String? = null
    private var pricePerDay = 50.0
    private var minDays = 1
    private var maxDays = 365
    private var isBanner = true
    private var promoId = 0
    private var promoDiscount = 0.0
    private var promoBonus = 0.0
    private var balanceMain = 0.0
    private var balanceAd = 0.0

    private var pickBytes: ByteArray? = null
    private var pickName = ""
    private var pickMime = ""

    private var blinkAnim: ValueAnimator? = null

    private val pickBanner = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) handlePicked(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_campaigns, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sp = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        savedToken = sp.getString("token", null)

        view.findViewById<TextView>(R.id.tv_camp_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val etTitle = view.findViewById<EditText>(R.id.et_camp_title)
        val etLink = view.findViewById<EditText>(R.id.et_camp_link)
        val etText = view.findViewById<EditText>(R.id.et_camp_text)
        val etColor = view.findViewById<EditText>(R.id.et_camp_color)
        val cbUseColor = view.findViewById<CheckBox>(R.id.cb_camp_usecolor)
        val cbBlink = view.findViewById<CheckBox>(R.id.cb_camp_blink)
        val tvPreview = view.findViewById<TextView>(R.id.tv_link_preview)
        val etHtml = view.findViewById<EditText>(R.id.et_html_code)
        val etDays = view.findViewById<EditText>(R.id.et_camp_days)
        val tvDaysNote = view.findViewById<TextView>(R.id.tv_days_note)
        val tvPromoInfo = view.findViewById<TextView>(R.id.tv_promo_info)
        val tvFinal = view.findViewById<TextView>(R.id.tv_final_calc)
        val tvBalance = view.findViewById<TextView>(R.id.tv_balance)
        val cbAgree = view.findViewById<CheckBox>(R.id.cb_agree_terms)
        val tvStatus = view.findViewById<TextView>(R.id.tv_camp_status)

        val updatePreview: () -> Unit = {
            updatePreview(tvPreview, etText, etColor, cbUseColor, cbBlink)
        }
        etText.addTextChangedListener(simpleWatcher(updatePreview))
        etColor.addTextChangedListener(simpleWatcher(updatePreview))
        cbUseColor.setOnCheckedChangeListener { _, _ -> updatePreview() }
        cbBlink.setOnCheckedChangeListener { _, _ -> updatePreview() }

        // Переключение типа: баннер / текстовая ссылка (как на вебе, баннер по умолчанию)
        val spType = view.findViewById<Spinner>(R.id.sp_camp_type)
        val types = listOf("Баннер (изображение)", "Текстовая ссылка")
        val typeAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, types)
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spType.adapter = typeAdapter
        val layText = view.findViewById<LinearLayout>(R.id.lay_text_fields)
        val layBanner = view.findViewById<LinearLayout>(R.id.lay_banner_fields)
        spType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                isBanner = pos == 0
                layText.visibility = if (isBanner) View.GONE else View.VISIBLE
                layBanner.visibility = if (isBanner) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        view.findViewById<Button>(R.id.btn_pick_banner).setOnClickListener {
            pickBanner.launch("image/*")
        }

        // Конфиг: цены, границы срока, балансы
        thread {
            try {
                val json = Api.post("campaigns.php", mapOf("action" to "config"), savedToken)
                if (json.optBoolean("ok")) {
                    pricePerDay = json.optDouble("price_per_day", 50.0)
                    minDays = json.optInt("min_days", 1)
                    maxDays = json.optInt("max_days", 365)
                    balanceMain = json.optDouble("balance", 0.0)
                    balanceAd = json.optDouble("ad_balance", 0.0)
                    activity?.runOnUiThread {
                        tvDaysNote.text = "Цена дня — " + trimNum(pricePerDay) + " ₽. Итог = срок × цена дня. Оплата за весь период: остаток не возвращается. Если модерация отклонит кампанию до запуска — деньги вернутся на баланс."
                        tvBalance.text = balanceLine()
                        updateFinal(tvFinal, etDays.text.toString())
                    }
                }
            } catch (_: Exception) {}
        }

        etDays.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateFinal(tvFinal, s?.toString() ?: "")
            }
        })

        buildCats(view)

        // Проверка промокода
        val etPromo = view.findViewById<EditText>(R.id.et_promo)
        view.findViewById<Button>(R.id.btn_check_promo).setOnClickListener {
            val code = etPromo.text.toString().trim().uppercase()
            if (code.isEmpty()) {
                promoId = 0; promoDiscount = 0.0; promoBonus = 0.0
                tvPromoInfo.text = ""
                updateFinal(tvFinal, etDays.text.toString())
                return@setOnClickListener
            }
            tvPromoInfo.text = "⏳ Проверяем промокод..."
            thread {
                try {
                    val json = Api.post("campaigns.php", mapOf("action" to "check_promo", "check_promo" to code), savedToken)
                    activity?.runOnUiThread {
                        if (json.optBoolean("ok")) {
                            promoId = json.optInt("id")
                            promoDiscount = json.optDouble("discount", 0.0)
                            promoBonus = json.optDouble("bonus", 0.0)
                            var info = "✅ Промокод активирован!"
                            if (promoDiscount > 0) info += "\nСкидка: " + trimNum(promoDiscount) + "%"
                            if (promoBonus > 0) info += "\nБонус: " + trimNum(promoBonus) + " ₽"
                            tvPromoInfo.text = info
                        } else {
                            promoId = 0; promoDiscount = 0.0; promoBonus = 0.0
                            tvPromoInfo.text = "❌ " + json.optString("msg", "Ошибка проверки промокода")
                        }
                        updateFinal(tvFinal, etDays.text.toString())
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        promoId = 0
                        tvPromoInfo.text = "❌ Ошибка проверки промокода"
                    }
                }
            }
        }

        view.findViewById<Button>(R.id.btn_camp_create).setOnClickListener {
            val days = etDays.text.toString().toIntOrNull() ?: 0
            val pickedCats = selectedCats.toList()
            when {
                days < minDays || days > maxDays -> tvStatus.text = "Срок размещения должен быть от " + minDays + " до " + maxDays + " дней"
                pickedCats.isEmpty() -> tvStatus.text = "Выберите хотя бы одну категорию площадок"
                etTitle.text.toString().trim().isEmpty() -> tvStatus.text = "Введите название кампании"
                !isBanner && etText.text.toString().trim().length < 3 -> tvStatus.text = "Укажите текст ссылки (минимум 3 символа)"
                !isBanner && etLink.text.toString().trim().isEmpty() -> tvStatus.text = "Укажите ссылку, куда ведёт реклама"
                isBanner && pickBytes == null && etHtml.text.toString().trim().isEmpty() -> tvStatus.text = "Загрузите файл баннера или укажите HTML-код"
                isBanner && pickBytes != null && etLink.text.toString().trim().isEmpty() -> tvStatus.text = "Укажите ссылку, куда ведёт баннер"
                !cbAgree.isChecked -> tvStatus.text = "Необходимо согласиться с условиями размещения рекламы"
                else -> {
                    tvStatus.text = "Создаём…"
                    createCampaign(view, etTitle, etLink, etText, etColor, cbUseColor, cbBlink, etHtml, etPromo, days, pickedCats, tvStatus)
                }
            }
        }

        load(view)
    }

    private fun handlePicked(uri: Uri) {
        thread {
            var err = ""
            var bytes: ByteArray? = null
            var name = "banner"
            var mime = ""
            try {
                val resolver = requireContext().contentResolver
                mime = resolver.getType(uri) ?: ""
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                val w = opts.outWidth
                val h = opts.outHeight
                if (w != 468 || h != 60) {
                    err = "Размер баннера должен быть ровно 468×60 (сейчас " + w + "×" + h + ")"
                } else {
                    resolver.openInputStream(uri)?.use { bytes = it.readBytes() }
                    if (bytes != null && bytes!!.size > 2 * 1024 * 1024) {
                        err = "Файл больше 2 МБ"
                        bytes = null
                    } else {
                        val ext = when (mime) {
                            "image/png" -> "png"
                            "image/jpeg" -> "jpg"
                            "image/gif" -> "gif"
                            "image/webp" -> "webp"
                            else -> ""
                        }
                        if (ext.isEmpty()) err = "Разрешены только PNG, JPEG, GIF и WebP"
                        name = "banner." + ext
                    }
                }
            } catch (e: Exception) {
                err = "Не удалось прочитать файл"
            }
            activity?.runOnUiThread {
                pickBytes = bytes
                pickName = name
                pickMime = mime
                val tv = view?.findViewById<TextView>(R.id.tv_banner_file)
                if (tv != null) {
                    if (bytes != null) {
                        tv.text = "Выбран: " + name + ", " + (bytes!!.size / 1024) + " КБ, 468×60 ✓"
                    } else {
                        tv.text = "Файл не выбран. " + err
                    }
                }
            }
        }
    }

    private fun createCampaign(
        view: View,
        etTitle: EditText, etLink: EditText, etText: EditText,
        etColor: EditText, cbUseColor: CheckBox, cbBlink: CheckBox,
        etHtml: EditText, etPromo: EditText,
        days: Int, pickedCats: List<String>, tvStatus: TextView
    ) {
        val params = mapOf(
            "action" to "create",
            "type" to (if (isBanner) "banner" else "link"),
            "name" to etTitle.text.toString().trim(),
            "days" to days.toString(),
            "categories" to pickedCats.joinToString(","),
            "agree_terms" to "1",
            "promo_code" to etPromo.text.toString().trim(),
            "link_text" to etText.text.toString().trim(),
            "link_url" to etLink.text.toString().trim(),
            "link_color" to etColor.text.toString().trim(),
            "use_color" to (if (cbUseColor.isChecked) "1" else "0"),
            "link_blink" to (if (cbBlink.isChecked) "1" else "0"),
            "html_code" to etHtml.text.toString().trim(),
            "banner_url" to etLink.text.toString().trim()
        )
        thread {
            try {
                val json = if (isBanner && pickBytes != null) {
                    ApiUpload.post(requireContext(), "campaigns.php", params, "banner_file", pickName, pickMime, pickBytes)
                } else {
                    Api.post("campaigns.php", params, savedToken)
                }
                activity?.runOnUiThread {
                    if (json.optBoolean("ok")) {
                        etTitle.setText("")
                        etText.setText("")
                        etColor.setText("")
                        cbUseColor.isChecked = false
                        cbBlink.isChecked = false
                        etHtml.setText("")
                        etPromo.setText("")
                        pickBytes = null
                        pickName = ""
                        selectedCats.clear()
                        view.findViewById<TextView>(R.id.tv_banner_file).text = "Файл не выбран"
                        val disc = json.optString("discount", "0").toDoubleOrNull() ?: 0.0
                        val bonus = json.optString("bonus", "0").toDoubleOrNull() ?: 0.0
                        var okMsg = "✅ Кампания создана и отправлена на модерацию! К списанию: " + json.optString("total", "") + " ₽"
                        if (disc > 0) okMsg += " • Скидка " + trimNum(disc) + " ₽"
                        if (bonus > 0) okMsg += " • Бонус " + trimNum(bonus) + " ₽"
                        tvStatus.text = okMsg
                        thread {
                            try {
                                val cfg = Api.post("campaigns.php", mapOf("action" to "config"), savedToken)
                                if (cfg.optBoolean("ok")) {
                                    balanceMain = cfg.optDouble("balance", 0.0)
                                    balanceAd = cfg.optDouble("ad_balance", 0.0)
                                    activity?.runOnUiThread {
                                        view.findViewById<TextView>(R.id.tv_balance).text = balanceLine()
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                        load(view)
                    } else {
                        tvStatus.text = json.optString("error", "Ошибка")
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread { tvStatus.text = "Нет связи с сервером" }
            }
        }
    }

    private fun buildCats(view: View) {
        val box = view.findViewById<LinearLayout>(R.id.cats_box)
        box.removeAllViews()
        selectedCats.clear()
        for (item in catItems) {
            val row = LinearLayout(requireContext())
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(8, 20, 8, 20)
            row.tag = item.code

            val sq = TextView(requireContext())
            sq.text = "☐"
            sq.textSize = 20f
            sq.setTextColor(Color.parseColor("#2563eb"))
            sq.setPadding(0, 0, 24, 0)

            val nameTv = TextView(requireContext())
            nameTv.text = item.name
            nameTv.textSize = 14f
            nameTv.setTextColor(Color.parseColor("#1E293B"))

            row.addView(sq)
            row.addView(nameTv)
            row.setOnClickListener {
                val code = row.tag as String
                val now = !selectedCats.contains(code)
                if (now) selectedCats.add(code) else selectedCats.remove(code)
                sq.text = if (now) "☑" else "☐"
                nameTv.setTypeface(nameTv.typeface, if (now) Typeface.BOLD else Typeface.NORMAL)
            }
            box.addView(row)
        }
    }

    private fun updatePreview(tv: TextView, etText: EditText, etColor: EditText, cbUseColor: CheckBox, cbBlink: CheckBox) {
        blinkAnim?.cancel()
        blinkAnim = null
        tv.text = if (etText.text.toString().trim().isNotEmpty()) etText.text.toString().trim() else "Ваш текст ссылки"
        tv.paintFlags = tv.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        tv.setTextColor(if (cbUseColor.isChecked && etColor.text.toString().matches(Regex("#[0-9a-fA-F]{6}"))) {
            Color.parseColor(etColor.text.toString())
        } else Color.parseColor("#2563eb"))
        if (cbBlink.isChecked) {
            val a = ValueAnimator.ofFloat(1f, 0.3f)
            a.duration = 500
            a.repeatCount = ValueAnimator.INFINITE
            a.repeatMode = ValueAnimator.REVERSE
            a.addUpdateListener { tv.alpha = it.animatedValue as Float }
            a.start()
            blinkAnim = a
        } else {
            tv.alpha = 1f
        }
    }

    private fun updateFinal(tvFinal: TextView, daysRaw: String) {
        val d = daysRaw.toIntOrNull() ?: 0
        if (d <= 0 || (promoId == 0 && promoBonus <= 0)) {
            tvFinal.visibility = View.GONE
            return
        }
        val cost = d * pricePerDay
        val discount = cost * promoDiscount / 100.0
        val final = cost - discount + promoBonus
        var txt = "Срок: " + d + " дн.\nСтоимость: " + trimNum(cost) + " ₽"
        if (discount > 0) txt += "\nСкидка: -" + trimNum(discount) + " ₽"
        if (promoBonus > 0) txt += "\nБонус: +" + trimNum(promoBonus) + " ₽"
        txt += "\nК списанию: " + trimNum(final) + " ₽"
        tvFinal.text = txt
        tvFinal.visibility = View.VISIBLE
    }

    private fun balanceLine(): String {
        var t = "Ваш баланс: " + trimNum(balanceMain) + " ₽"
        if (balanceAd > 0) t += " + рекламный баланс: " + trimNum(balanceAd) + " ₽"
        return t
    }

    private fun simpleWatcher(f: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { f() }
        }
    }

    private fun trimNum(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
    }

    private fun load(view: View) {
        val listHolder = view.findViewById<LinearLayout>(R.id.campaigns_list)
        thread {
            var list: List<org.json.JSONObject> = emptyList()
            var error = ""
            try {
                val json = Api.post("campaigns.php", mapOf("action" to "list"), savedToken)
                if (json.optBoolean("ok")) {
                    val arr = json.optJSONArray("campaigns")
                    for (i in 0 until (arr?.length() ?: 0)) list += arr!!.getJSONObject(i)
                } else error = json.optString("error", "Ошибка")
            } catch (e: Exception) {
                error = "Нет связи с сервером"
            }
            activity?.runOnUiThread {
                listHolder.removeAllViews()
                if (error.isNotEmpty()) {
                    listHolder.addView(label(error, Color.parseColor("#EF4444")))
                    return@runOnUiThread
                }
                if (list.isEmpty()) {
                    listHolder.addView(label("Кампаний пока нет. Создайте первую выше.", Color.parseColor("#64748B")))
                    return@runOnUiThread
                }
                for (c in list) listHolder.addView(card(c))
            }
        }
    }

    private fun label(text: String, color: Int): TextView {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 14f
        tv.setTextColor(color)
        return tv
    }

    private fun card(c: org.json.JSONObject): View {
        val ctx = requireContext()
        val box = LinearLayout(ctx)
        box.orientation = LinearLayout.VERTICAL
        box.background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 40f }
        box.setPadding(40, 36, 40, 36)
        box.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 20 }

        val title = TextView(ctx)
        title.text = (if (c.optString("type") == "banner") "🖼 " else "🔗 ") + c.optString("name")
        title.textSize = 16f
        title.setTextColor(Color.parseColor("#1E293B"))
        title.setTypeface(title.typeface, Typeface.BOLD)

        val link = TextView(ctx)
        link.text = c.optString("link")
        link.textSize = 13f
        link.setTextColor(Color.parseColor("#64748B"))

        if (c.optString("type") == "banner" && c.optString("image").isNotEmpty()) {
            val imgNote = TextView(ctx)
            imgNote.text = "Баннер: " + c.optString("image")
            imgNote.textSize = 13f
            imgNote.setTextColor(Color.parseColor("#64748B"))
            box.addView(imgNote)
        }

        val catsV = TextView(ctx)
        catsV.text = "Категории: " + c.optString("cats", "—")
        catsV.textSize = 13f
        catsV.setTextColor(Color.parseColor("#64748B"))

        val money = TextView(ctx)
        money.text = "Срок: " + c.optInt("days").toString() + " дн. • Оплачено: " + c.optString("paid") + " ₽"
        if (c.optString("start").isNotEmpty()) {
            money.text = money.text.toString() + " • " + c.optString("start") + " → " + c.optString("end")
        }
        money.textSize = 13f
        money.setTextColor(Color.parseColor("#64748B"))

        val stats = TextView(ctx)
        stats.text = "👁 " + c.optInt("impr") + " • 🖱 " + c.optInt("clicks") + " • CTR: " + trimNum(c.optDouble("ctr", 0.0)) + "%"
        stats.textSize = 13f
        stats.setTextColor(Color.parseColor("#334155"))

        val st = TextView(ctx)
        st.textSize = 13f
        when (c.optString("status")) {
            "active"  -> { st.text = "✅ Активна"; st.setTextColor(Color.parseColor("#10B981")) }
            "pending" -> { st.text = "⏳ На модерации"; st.setTextColor(Color.parseColor("#F59E0B")) }
            "paused"  -> { st.text = "⏸ На паузе"; st.setTextColor(Color.parseColor("#64748B")) }
            "expired" -> { st.text = "🏁 Завершена"; st.setTextColor(Color.parseColor("#64748B")) }
            "rejected"-> {
                st.text = "❌ Отклонена" + (if (c.optString("reason").isNotEmpty()) ": " + c.optString("reason") else "") + " • Деньги возвращены"
                st.setTextColor(Color.parseColor("#EF4444"))
            }
            else -> st.text = c.optString("status")
        }

        box.addView(title)
        if (c.optString("ad_text").isNotEmpty() && c.optString("type") == "link") {
            val adText = TextView(ctx)
            adText.text = c.optString("ad_text")
            adText.textSize = 13f
            adText.setTextColor(Color.parseColor("#334155"))
            box.addView(adText)
        }
        box.addView(link)
        box.addView(catsV)
        box.addView(money)
        box.addView(stats)
        box.addView(st)

        if (c.optString("status") == "active") {
            val btn = Button(ctx)
            btn.text = "Пауза"
            btn.textSize = 13f
            btn.setOnClickListener {
                thread {
                    Api.post("campaigns.php", mapOf("action" to "stop", "id" to c.optInt("id").toString()), savedToken)
                    activity?.runOnUiThread { view?.let { load(it) } }
                }
            }
            box.addView(btn)
        }
        if (c.optString("status") == "paused") {
            val btn = Button(ctx)
            btn.text = "Возобновить"
            btn.textSize = 13f
            btn.setOnClickListener {
                thread {
                    Api.post("campaigns.php", mapOf("action" to "resume", "id" to c.optInt("id").toString()), savedToken)
                    activity?.runOnUiThread { view?.let { load(it) } }
                }
            }
            box.addView(btn)
        }
        return box
    }
}

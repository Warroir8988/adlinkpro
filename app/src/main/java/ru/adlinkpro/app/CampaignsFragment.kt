package ru.adlinkpro.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class CampaignsFragment : Fragment() {
    private data class Site(val id: Int, val label: String)
    private var sites: List<Site> = emptyList()
    private var savedToken: String? = null
    private var pricePerDay = 0.0
    private var minDays = 1
    private var maxDays = 365
    private var selectedCats = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_campaigns, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sp = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        savedToken = sp.getString("token", null)

        view.findViewById<TextView>(R.id.tv_camp_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val spSite = view.findViewById<Spinner>(R.id.sp_camp_site)
        val etTitle = view.findViewById<EditText>(R.id.et_camp_title)
        val etText = view.findViewById<EditText>(R.id.et_camp_text)
        val etDays = view.findViewById<EditText>(R.id.et_camp_days)
        val tvPrice = view.findViewById<TextView>(R.id.tv_camp_price)
        val tvStatus = view.findViewById<TextView>(R.id.tv_camp_status)

        // Цена и границы из админ-настроек
        thread {
            try {
                val json = Api.post("campaigns.php", mapOf("action" to "config"), savedToken)
                if (json.optBoolean("ok")) {
                    pricePerDay = json.optDouble("price_per_day", 50.0)
                    minDays = json.optInt("min_days", 1)
                    maxDays = json.optInt("max_days", 365)
                    activity?.runOnUiThread {
                        tvPrice.text = "Цена дня: " + trimNum(pricePerDay) + " ₽ • Срок: " + minDays + "–" + maxDays + " дн. • Итого: 0 ₽"
                    }
                }
            } catch (_: Exception) {}
        }

        // Живой пересчёт итога
        etDays.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val d = s?.toString()?.toIntOrNull() ?: 0
                val total = if (d in 1..maxDays) d * pricePerDay else 0.0
                tvPrice.text = "Цена дня: " + trimNum(pricePerDay) + " ₽ • Срок: " + minDays + "–" + maxDays + " дн. • Итого: " + trimNum(total) + " ₽"
            }
        })

        // Выбор категорий (те же имена, что на экране сайтов)
        thread {
            var names: List<String> = emptyList()
            try {
                val json = Api.post("sites.php", mapOf("action" to "categories"), savedToken)
                if (json.optBoolean("ok")) {
                    val arr = json.optJSONArray("categories")
                    for (i in 0 until (arr?.length() ?: 0)) names += arr!!.getString(i)
                }
            } catch (_: Exception) {}
            if (names.isNotEmpty()) {
                val cats = names
                activity?.runOnUiThread {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    // категории выбираются тапом по строке цены — открываем диалог
                    tvPrice.setOnClickListener {
                        val chosen = selectedCats.split(",").filter { it.isNotBlank() }.toSet()
                        val checked = cats.map { it in chosen }.toBooleanArray()
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Категории показа")
                            .setMultiChoiceItems(cats.toTypedArray(), checked) { _, which, isChecked ->
                                val set = selectedCats.split(",").filter { it.isNotBlank() }.toMutableSet()
                                if (isChecked) set.add(cats[which]) else set.remove(cats[which])
                                selectedCats = set.joinToString(", ")
                            }
                            .setPositiveButton("Выбрать") { d, _ -> d.dismiss() }
                            .setNegativeButton("Отмена") { d, _ -> d.dismiss() }
                            .show()
                    }
                }
            }
        }

        view.findViewById<Button>(R.id.btn_camp_create).setOnClickListener {
            if (sites.isEmpty()) {
                tvStatus.text = "Нет одобренных сайтов — сначала добавьте сайт"
                return@setOnClickListener
            }
            val days = etDays.text.toString().toIntOrNull() ?: 0
            if (days < minDays || days > maxDays) {
                tvStatus.text = "Срок: от " + minDays + " до " + maxDays + " дней"
                return@setOnClickListener
            }
            val site = sites[spSite.selectedItemPosition]
            tvStatus.text = "Создаём…"
            thread {
                try {
                    val json = Api.post(
                        "campaigns.php",
                        mapOf(
                            "action" to "create",
                            "site_id" to site.id.toString(),
                            "name" to etTitle.text.toString().trim(),
                            "ad_text" to etText.text.toString().trim(),
                            "days" to days.toString(),
                            "categories" to selectedCats
                        ),
                        savedToken
                    )
                    activity?.runOnUiThread {
                        if (json.optBoolean("ok")) {
                            etTitle.setText("")
                            etText.setText("")
                            etDays.setText("")
                            tvStatus.text = ""
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

        loadSites(view)
        load(view)
    }

    private fun trimNum(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
    }

    private fun loadSites(view: View) {
        thread {
            val found = mutableListOf<Site>()
            try {
                val json = Api.post("sites.php", mapOf("action" to "list"), savedToken)
                if (json.optBoolean("ok")) {
                    val arr = json.optJSONArray("sites")
                    for (i in 0 until (arr?.length() ?: 0)) {
                        val s = arr!!.getJSONObject(i)
                        if (s.optString("status") == "approved") {
                            found += Site(s.optInt("id"), s.optString("name") + " — " + s.optString("url"))
                        }
                    }
                }
            } catch (_: Exception) {}
            activity?.runOnUiThread {
                sites = found
                val labels = if (found.isEmpty()) listOf("Нет одобренных сайтов") else found.map { it.label }
                val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, labels)
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                view.findViewById<Spinner>(R.id.sp_camp_site).adapter = adapter
            }
        }
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
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 40f }
            setPadding(40, 36, 40, 36)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }
        val title = TextView(ctx)
        title.text = c.optString("name")
        title.textSize = 16f
        title.setTextColor(Color.parseColor("#1E293B"))
        title.setTypeface(title.typeface, Typeface.BOLD)

        val site = TextView(ctx)
        site.text = "Сайт: " + c.optString("site_url", c.optString("link"))
        site.textSize = 13f
        site.setTextColor(Color.parseColor("#64748B"))

        val adText = TextView(ctx)
        adText.text = c.optString("ad_text")
        adText.textSize = 13f
        adText.setTextColor(Color.parseColor("#334155"))

        val cats = TextView(ctx)
        cats.text = "Категории: " + c.optString("cats", "—")
        cats.textSize = 13f
        cats.setTextColor(Color.parseColor("#64748B"))

        val money = TextView(ctx)
        money.text = "Срок: " + c.optInt("days").toString() + " дн. • Бюджет: " + c.optString("budget") + " ₽"
        if (c.optString("start").isNotEmpty()) {
            money.text = money.text.toString() + " • " + c.optString("start") + " → " + c.optString("end")
        }
        money.textSize = 13f
        money.setTextColor(Color.parseColor("#64748B"))

        val st = TextView(ctx)
        st.textSize = 13f
        when (c.optString("status")) {
            "active"  -> { st.text = "▶️ Активна"; st.setTextColor(Color.parseColor("#10B981")) }
            "pending" -> { st.text = "⏳ На модерации"; st.setTextColor(Color.parseColor("#F59E0B")) }
            "paused"  -> { st.text = "⏸ На паузе"; st.setTextColor(Color.parseColor("#64748B")) }
            "expired" -> { st.text = "🏁 Завершена"; st.setTextColor(Color.parseColor("#64748B")) }
            "rejected"-> {
                st.text = "❌ Отклонена" + (if (c.optString("reason").isNotEmpty()) ": " + c.optString("reason") else "") + " • Деньги возвращены"
                st.setTextColor(Color.parseColor("#EF4444"))
            }
            else -> st.text = c.optString("status")
        }

        box.addView(title); box.addView(site); box.addView(adText); box.addView(cats); box.addView(money); box.addView(st)

        if (c.optString("status") == "active") {
            val btn = Button(ctx).apply {
                this.text = "Пауза"
                textSize = 13f
                setOnClickListener {
                    thread {
                        Api.post("campaigns.php", mapOf("action" to "stop", "id" to c.optInt("id").toString()), savedToken)
                        activity?.runOnUiThread { view?.let { load(it) } }
                    }
                }
            }
            box.addView(btn)
        }
        return box
    }
}

package ru.adlinkpro.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
        val etBudget = view.findViewById<EditText>(R.id.et_camp_budget)
        val tvStatus = view.findViewById<TextView>(R.id.tv_camp_status)

        view.findViewById<Button>(R.id.btn_camp_create).setOnClickListener {
            if (sites.isEmpty()) {
                tvStatus.text = "Нет одобренных сайтов — сначала добавьте сайт"
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
                            "title" to etTitle.text.toString().trim(),
                            "ad_text" to etText.text.toString().trim(),
                            "budget" to etBudget.text.toString().trim()
                        ),
                        savedToken
                    )
                    activity?.runOnUiThread {
                        if (json.optBoolean("ok")) {
                            etTitle.setText("")
                            etText.setText("")
                            etBudget.setText("")
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
        val title = TextView(ctx).apply { text = c.optString("title"); textSize = 16f; setTextColor(Color.parseColor("#1E293B")); setTypeface(typeface, Typeface.BOLD) }
        val site = TextView(ctx).apply { text = "Сайт: " + c.optString("site_name", c.optString("site_url")); textSize = 13f; setTextColor(Color.parseColor("#64748B")) }
        val text = TextView(ctx).apply { text = c.optString("ad_text"); textSize = 13f; setTextColor(Color.parseColor("#334155")) }
        val money = TextView(ctx).apply {
            text = "Бюджет: " + c.optString("budget") + " ₽ • Израсходовано: " + c.optString("spent") + " ₽ • Клики: " + c.optInt("clicks").toString()
            textSize = 13f; setTextColor(Color.parseColor("#64748B"))
        }
        val st = TextView(ctx).apply {
            textSize = 13f
            when (c.optString("status")) {
                "active"  -> { text = "▶️ Активна"; setTextColor(Color.parseColor("#10B981")) }
                "pending" -> { text = "⏳ На модерации"; setTextColor(Color.parseColor("#F59E0B")) }
                "stopped" -> { text = "⏸ Остановлена"; setTextColor(Color.parseColor("#64748B")) }
                "rejected"-> { text = "❌ Отклонена" + (if (c.optString("reason").isNotEmpty()) ": " + c.optString("reason") else ""); setTextColor(Color.parseColor("#EF4444")) }
                else -> text = c.optString("status")
            }
        }
        box.addView(title); box.addView(site); box.addView(text); box.addView(money); box.addView(st)

        if (c.optString("status") == "active") {
            val btn = Button(ctx).apply {
                text = "Остановить"
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

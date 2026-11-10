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

class SitesFragment : Fragment() {
    private var cats: List<String> = listOf("Прочее")
    private var savedToken: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_sites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sp = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        savedToken = sp.getString("token", null)

        view.findViewById<TextView>(R.id.tv_sites_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val spCat = view.findViewById<Spinner>(R.id.sp_site_cat)
        val etUrl = view.findViewById<EditText>(R.id.et_site_url)
        val etName = view.findViewById<EditText>(R.id.et_site_name)
        val tvStatus = view.findViewById<TextView>(R.id.tv_site_status)

        view.findViewById<Button>(R.id.btn_site_add).setOnClickListener {
            val cat = if (spCat.selectedItemPosition >= 0) cats[spCat.selectedItemPosition] else "Прочее"
            tvStatus.text = "Отправляем…"
            thread {
                try {
                    val json = Api.post(
                        "sites.php",
                        mapOf("action" to "add", "url" to etUrl.text.toString().trim(),
                              "name" to etName.text.toString().trim(), "category" to cat),
                        savedToken
                    )
                    activity?.runOnUiThread {
                        if (json.optBoolean("ok")) {
                            etUrl.setText("")
                            etName.setText("")
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

        // Категории с сервера — как на сайте
        thread {
            var names: List<String> = emptyList()
            try {
                val json = Api.post("sites.php", mapOf("action" to "categories"), savedToken)
                if (json.optBoolean("ok")) {
                    val arr = json.optJSONArray("categories")
                    for (i in 0 until (arr?.length() ?: 0)) names += arr!!.getString(i)
                }
            } catch (_: Exception) {}
            if (names.isEmpty()) names = listOf("Прочее")
            cats = names
            activity?.runOnUiThread {
                val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, cats)
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                spCat.adapter = adapter
            }
        }

        load(view)
    }

    private fun load(view: View) {
        val listHolder = view.findViewById<LinearLayout>(R.id.sites_list)
        thread {
            var sites: List<org.json.JSONObject> = emptyList()
            var error = ""
            try {
                val json = Api.post("sites.php", mapOf("action" to "list"), savedToken)
                if (json.optBoolean("ok")) {
                    val arr = json.optJSONArray("sites")
                    for (i in 0 until (arr?.length() ?: 0)) sites += arr!!.getJSONObject(i)
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
                if (sites.isEmpty()) {
                    listHolder.addView(label("Сайтов пока нет. Добавьте первый выше.", Color.parseColor("#64748B")))
                    return@runOnUiThread
                }
                for (s in sites) listHolder.addView(card(s))
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

    private fun card(s: org.json.JSONObject): View {
        val ctx = requireContext()
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 40f }
            setPadding(40, 36, 40, 36)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }
        val name = TextView(ctx).apply { text = s.optString("name"); textSize = 16f; setTextColor(Color.parseColor("#1E293B")); setTypeface(typeface, Typeface.BOLD) }
        val url = TextView(ctx).apply { text = s.optString("url"); textSize = 13f; setTextColor(Color.parseColor("#64748B")) }
        val cat = TextView(ctx).apply { text = "Категория: " + s.optString("category"); textSize = 13f; setTextColor(Color.parseColor("#64748B")) }
        val st = TextView(ctx).apply {
            textSize = 13f
            when (s.optString("status")) {
                "approved" -> { text = "✅ Одобрен"; setTextColor(Color.parseColor("#10B981")) }
                "pending"  -> { text = "⏳ На проверке"; setTextColor(Color.parseColor("#F59E0B")) }
                "rejected" -> { text = "❌ Отклонён" + (if (s.optString("reason").isNotEmpty()) ": " + s.optString("reason") else ""); setTextColor(Color.parseColor("#EF4444")) }
                else -> text = s.optString("status")
            }
        }
        box.addView(name); box.addView(url); box.addView(cat); box.addView(st)

        if (s.optString("status") != "approved") {
            val btn = Button(ctx).apply {
                text = "Удалить"
                textSize = 13f
                setOnClickListener {
                    thread {
                        Api.post("sites.php", mapOf("action" to "delete", "id" to s.optInt("id").toString()), savedToken)
                        activity?.runOnUiThread { view?.let { load(it) } }
                    }
                }
            }
            box.addView(btn)
        }
        return box
    }
}

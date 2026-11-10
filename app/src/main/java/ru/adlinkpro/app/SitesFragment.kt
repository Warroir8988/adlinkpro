package ru.adlinkpro.app

import android.content.Context
import android.os.Bundle
import android.view.Gravity
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
    private val cats = listOf("other", "news", "finance", "crypto", "games", "shopping", "education", "entertainment")
    private val catNames = listOf("Другое", "Новости", "Финансы", "Крипто", "Игры", "Покупки", "Обучение", "Развлечения")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_sites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sp = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        val token = sp.getString("token", null)

        view.findViewById<TextView>(R.id.tv_sites_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val spCat = view.findViewById<Spinner>(R.id.sp_site_cat)
        spCat.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, catNames)

        val etUrl = view.findViewById<EditText>(R.id.et_site_url)
        val etName = view.findViewById<EditText>(R.id.et_site_name)
        val tvStatus = view.findViewById<TextView>(R.id.tv_site_status)

        view.findViewById<Button>(R.id.btn_site_add).setOnClickListener {
            val cat = cats[spCat.selectedItemPosition]
            tvStatus.text = "Отправляем…"
            thread {
                try {
                    val json = Api.post(
                        "sites.php",
                        mapOf("action" to "add", "url" to etUrl.text.toString().trim(),
                              "name" to etName.text.toString().trim(), "category" to cat),
                        token
                    )
                    activity?.runOnUiThread {
                        if (json.optBoolean("ok")) {
                            etUrl.setText("")
                            etName.setText("")
                            tvStatus.text = ""
                            load(view, token)
                        } else {
                            tvStatus.text = json.optString("error", "Ошибка")
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread { tvStatus.text = "Нет связи с сервером" }
                }
            }
        }

        load(view, token)
    }

    private fun load(view: View, token: String?) {
        val listHolder = view.findViewById<LinearLayout>(R.id.sites_list)
        thread {
            var sites: List<org.json.JSONObject> = emptyList()
            var error = ""
            try {
                val json = Api.post("sites.php", mapOf("action" to "list"), token)
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
                    listHolder.addView(label(error, "@color/red"))
                    return@runOnUiThread
                }
                if (sites.isEmpty()) {
                    listHolder.addView(label("Сайтов пока нет. Добавьте первый выше.", "@color/text_secondary"))
                    return@runOnUiThread
                }
                for (s in sites) listHolder.addView(card(s))
            }
        }
    }

    private fun label(text: String, colorRef: String): TextView {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 14f
        tv.setTextColor(resources.getColor(resources.getIdentifier(colorRef.removePrefix("@color/"), "color", requireContext().packageName), requireContext().theme))
        return tv
    }

    private fun card(s: org.json.JSONObject): View {
        val ctx = requireContext()
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(40, 32, 40, 32)
            // отступ между карточками
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }
        val name = TextView(ctx).apply { text = s.optString("name"); textSize = 16f; setTextColor(0xFF1E293B.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD) }
        val url = TextView(ctx).apply { text = s.optString("url"); textSize = 13f; setTextColor(0xFF64748B.toInt()) }
        val st = TextView(ctx).apply {
            textSize = 13f
            when (s.optString("status")) {
                "approved" -> { text = "✅ Одобрен"; setTextColor(0xFF10B981.toInt()) }
                "pending"  -> { text = "⏳ На проверке"; setTextColor(0xFFF59E0B.toInt()) }
                "rejected" -> { text = "❌ Отклонён" + (if (s.optString("reason").isNotEmpty()) ": " + s.optString("reason") else ""); setTextColor(0xFFEF4444.toInt()) }
                else -> text = s.optString("status")
            }
        }
        box.addView(name); box.addView(url); box.addView(st)

        if (s.optString("status") != "approved") {
            val btn = Button(ctx).apply {
                text = "Удалить"
                textSize = 13f
                setOnClickListener {
                    thread {
                        Api.post("sites.php", mapOf("action" to "delete", "id" to s.optInt("id").toString()), null)
                        activity?.runOnUiThread { load(requireView(), requireContext().getSharedPreferences("app", Context.MODE_PRIVATE).getString("token", null)) }
                    }
                }
            }
            box.addView(btn)
        }
        return box
    }
}

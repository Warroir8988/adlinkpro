package ru.adlinkpro.app

import android.content.ClipData
import android.content.ClipboardManager
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
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class ShowcaseFragment : Fragment() {
    private data class MySite(val id: Int, val label: String, val level: Int, val imp7: Int, val imp: Int, val clicks: Int, val hosts: Int)
    private var mySites: List<MySite> = emptyList()
    private var savedToken: String? = null
    private var priceImp = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_showcase, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sp = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        savedToken = sp.getString("token", null)

        view.findViewById<TextView>(R.id.tv_show_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val spSite = view.findViewById<Spinner>(R.id.sp_show_site)
        spSite.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (mySites.isEmpty() || pos >= mySites.size) return
                showCodes(view, mySites[pos])
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        view.findViewById<Button>(R.id.btn_copy_banner).setOnClickListener {
            copy(view.findViewById<TextView>(R.id.tv_code_banner).text.toString())
        }
        view.findViewById<Button>(R.id.btn_copy_text).setOnClickListener {
            copy(view.findViewById<TextView>(R.id.tv_code_text).text.toString())
        }

        loadMine(view)
        loadCatalog(view)
    }

    private fun copy(text: String) {
        if (text.isBlank()) return
        val cb = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("AdLinkPro", text))
        Toast.makeText(requireContext(), "Скопировано", Toast.LENGTH_SHORT).show()
    }

    private fun loadMine(view: View) {
        val tvStats = view.findViewById<TextView>(R.id.tv_show_stats)
        val tvPrice = view.findViewById<TextView>(R.id.tv_show_price)
        val spSite = view.findViewById<Spinner>(R.id.sp_show_site)
        thread {
            var sites: List<MySite> = emptyList()
            var error = ""
            try {
                val json = Api.post("showcase.php", mapOf("action" to "mine"), savedToken)
                if (json.optBoolean("ok")) {
                    priceImp = json.optDouble("price_per_impression", 0.10)
                    val arr = json.optJSONArray("sites")
                    for (i in 0 until (arr?.length() ?: 0)) {
                        val s = arr!!.getJSONObject(i)
                        sites += MySite(
                            s.optInt("id"),
                            s.optString("name") + " — " + s.optString("host"),
                            s.optInt("level"),
                            s.optInt("imp7"),
                            s.optInt("imp"),
                            s.optInt("clicks"),
                            s.optInt("hosts")
                        )
                    }
                } else error = json.optString("error", "Ошибка")
            } catch (e: Exception) {
                error = "Нет связи с сервером"
            }
            activity?.runOnUiThread {
                mySites = sites
                val labels = if (sites.isEmpty()) listOf("Нет одобренных площадок") else sites.map { it.label }
                val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, labels)
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                spSite.adapter = adapter
                if (sites.isNotEmpty()) {
                    tvPrice.text = "Цена показа сейчас — " + trimNum(priceImp) + " ₽. Код меняется от выбранной площадки."
                    showCodes(view, sites[0])
                } else {
                    tvPrice.text = "Добавьте и одобрите площадку — тогда появится код витрины."
                    tvStats.text = if (error.isNotEmpty()) error else ""
                }
            }
        }
    }

    private fun showCodes(view: View, s: MySite) {
        val tvBanner = view.findViewById<TextView>(R.id.tv_code_banner)
        val tvText = view.findViewById<TextView>(R.id.tv_code_text)
        val tvStats = view.findViewById<TextView>(R.id.tv_show_stats)
        tvBanner.text = "Загружаем…"
        tvText.text = "Загружаем…"
        val work = if (s.imp7 > 0) "🟢 Код работает: " + s.imp7 + " показов за 7 дней" else "⚪ Показов с этого домена не было за 7 дней"
        tvStats.text = "Уровень: ⭐ " + s.level + " • Показы 30 дн.: " + s.imp + " • Клики: " + s.clicks + " • Хосты: " + s.hosts + "\n" + work
        thread {
            try {
                val json = Api.post("showcase.php", mapOf("action" to "code", "site_id" to s.id.toString()), savedToken)
                activity?.runOnUiThread {
                    if (json.optBoolean("ok")) {
                        tvBanner.text = json.optString("banner_code")
                        tvText.text = json.optString("text_code")
                    } else {
                        tvBanner.text = json.optString("error", "Ошибка")
                        tvText.text = ""
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    tvBanner.text = "Нет связи с сервером"
                    tvText.text = ""
                }
            }
        }
    }

    private fun loadCatalog(view: View) {
        val holder = view.findViewById<LinearLayout>(R.id.showcase_catalog)
        thread {
            var list: List<org.json.JSONObject> = emptyList()
            try {
                val json = Api.post("showcase.php", mapOf("action" to "catalog"), "")
                if (json.optBoolean("ok")) {
                    val arr = json.optJSONArray("sites")
                    for (i in 0 until (arr?.length() ?: 0)) list += arr!!.getJSONObject(i)
                }
            } catch (_: Exception) {}
            activity?.runOnUiThread {
                holder.removeAllViews()
                if (list.isEmpty()) {
                    holder.addView(label("Пока нет одобренных площадок.", Color.parseColor("#64748B")))
                    return@runOnUiThread
                }
                for (s in list) holder.addView(catalogCard(s))
            }
        }
    }

    private fun catalogCard(s: org.json.JSONObject): View {
        val ctx = requireContext()
        val box = LinearLayout(ctx)
        box.orientation = LinearLayout.VERTICAL
        box.background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 40f }
        box.setPadding(40, 36, 40, 36)
        box.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 20 }

        val name = TextView(ctx)
        name.text = "🌐 " + s.optString("name")
        name.textSize = 15f
        name.setTextColor(Color.parseColor("#1E293B"))
        name.setTypeface(name.typeface, Typeface.BOLD)

        val host = TextView(ctx)
        host.text = s.optString("host") + " • " + s.optString("cat")
        host.textSize = 12f
        host.setTextColor(Color.parseColor("#64748B"))

        box.addView(name)
        box.addView(host)

        val d = s.optString("desc")
        if (d.isNotEmpty()) {
            val desc = TextView(ctx)
            desc.text = d
            desc.textSize = 12f
            desc.setTextColor(Color.parseColor("#64748B"))
            box.addView(desc)
        }

        val statsLine = "⭐ " + s.optInt("level") + " • Стаж: " + s.optInt("days") + " дн. • 👁 " + s.optInt("imp") +
            " • 🖱 " + s.optInt("clicks") + " • CTR: " + trimNum(s.optDouble("ctr", 0.0)) + "% • Хосты: " + s.optInt("hosts")
        val stats = TextView(ctx)
        stats.text = statsLine
        stats.textSize = 12f
        stats.setTextColor(Color.parseColor("#334155"))
        box.addView(stats)

        return box
    }

    private fun label(text: String, color: Int): TextView {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 14f
        tv.setTextColor(color)
        return tv
    }

    private fun trimNum(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
    }
}

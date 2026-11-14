package ru.adlinkpro.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class NewsFragment : Fragment() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()

        val scroll = ScrollView(ctx)
        scroll.setBackgroundColor(Color.parseColor("#f8fafc"))
        val content = LinearLayout(ctx)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(14), dp(14), dp(14), dp(24))
        scroll.addView(content)

        val title = TextView(ctx)
        title.text = "📰 Новости v2"
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.parseColor("#1e293b"))
        content.addView(title)

        val status = TextView(ctx)
        status.textSize = 13f
        status.setTextColor(Color.parseColor("#64748b"))
        status.visibility = View.GONE
        val statusLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        statusLp.topMargin = dp(12)
        status.layoutParams = statusLp
        content.addView(status)

        val listHolder = LinearLayout(ctx)
        listHolder.orientation = LinearLayout.VERTICAL
        content.addView(listHolder)

        load(listHolder, status)

        return scroll
    }

    private fun card(ctx: android.content.Context, title: String, text: String, date: String): LinearLayout {
        val box = LinearLayout(ctx)
        box.orientation = LinearLayout.VERTICAL
        val bg = GradientDrawable()
        bg.setColor(Color.WHITE)
        bg.cornerRadius = dp(14).toFloat()
        bg.setStroke(dp(1), Color.parseColor("#e2e8f0"))
        box.background = bg
        box.setPadding(dp(14), dp(12), dp(14), dp(12))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(10)
        box.layoutParams = lp

        val head = LinearLayout(ctx)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL

        val t = TextView(ctx)
        t.text = title
        t.textSize = 15f
        t.setTypeface(null, Typeface.BOLD)
        t.setTextColor(Color.parseColor("#1e293b"))
        t.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        head.addView(t)

        if (date.isNotEmpty()) {
            val d = TextView(ctx)
            d.text = date
            d.textSize = 11f
            d.setTextColor(Color.parseColor("#64748b"))
            head.addView(d)
        }
        box.addView(head)

        if (text.isNotEmpty()) {
            val msg = TextView(ctx)
            msg.text = text
            msg.textSize = 13f
            msg.setTextColor(Color.parseColor("#334155"))
            msg.setPadding(0, dp(8), 0, 0)
            box.addView(msg)
        }
        return box
    }

    private fun load(listHolder: LinearLayout, status: TextView) {
        val ctx = requireContext()
        val token = ctx.getSharedPreferences("app", Context.MODE_PRIVATE).getString("token", null)
        thread {
            var err = ""
            val items = mutableListOf<org.json.JSONObject>()
            try {
                val json = Api.post("news.php", emptyMap(), token)
                if (json.optBoolean("ok")) {
                    val arr = json.optJSONArray("items")
                    for (i in 0 until (arr?.length() ?: 0)) items += arr!!.getJSONObject(i)
                } else err = json.optString("error", "Ошибка")
            } catch (e: Exception) {
                err = "Нет связи с сервером"
            }
            activity?.runOnUiThread {
                listHolder.removeAllViews()
                if (err.isNotEmpty()) {
                    status.visibility = View.VISIBLE
                    status.text = err
                    return@runOnUiThread
                }
                status.visibility = View.GONE
                if (items.isEmpty()) {
                    val box = LinearLayout(ctx)
                    box.orientation = LinearLayout.VERTICAL
                    box.gravity = Gravity.CENTER
                    val bg = GradientDrawable()
                    bg.setColor(Color.WHITE)
                    bg.cornerRadius = dp(14).toFloat()
                    bg.setStroke(dp(1), Color.parseColor("#e2e8f0"))
                    box.background = bg
                    box.setPadding(dp(20), dp(28), dp(20), dp(28))
                    val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    lp.topMargin = dp(12)
                    box.layoutParams = lp

                    val big = TextView(ctx)
                    big.text = "📰"
                    big.textSize = 36f
                    big.gravity = Gravity.CENTER
                    box.addView(big)
                    val t = TextView(ctx)
                    t.text = "Новостей пока нет"
                    t.textSize = 15f
                    t.setTypeface(null, Typeface.BOLD)
                    t.gravity = Gravity.CENTER
                    t.setPadding(0, dp(12), 0, dp(6))
                    box.addView(t)
                    val sub = TextView(ctx)
                    sub.text = "Здесь будут появляться новости платформы: обновления, акции и изменения в работе."
                    sub.textSize = 13f
                    sub.setTextColor(Color.parseColor("#64748b"))
                    sub.gravity = Gravity.CENTER
                    box.addView(sub)
                    listHolder.addView(box)
                    return@runOnUiThread
                }
                for (n in items) {
                    listHolder.addView(card(
                        ctx,
                        n.optString("title", "Без заголовка"),
                        n.optString("text", ""),
                        n.optString("date", "")
                    ))
                }
            }
        }
    }
}

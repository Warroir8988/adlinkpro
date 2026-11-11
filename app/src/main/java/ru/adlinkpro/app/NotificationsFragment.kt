package ru.adlinkpro.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class NotificationsFragment : Fragment() {
    private var savedToken: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sp = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        savedToken = sp.getString("token", null)

        view.findViewById<TextView>(R.id.tv_notif_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<TextView>(R.id.btn_mark_all).setOnClickListener {
            thread {
                try {
                    Api.post("notifications.php", mapOf("action" to "mark_all"), savedToken)
                    activity?.runOnUiThread { load(view) }
                } catch (_: Exception) {}
            }
        }

        load(view)
    }

    private fun load(view: View) {
        val listHolder = view.findViewById<LinearLayout>(R.id.notif_list)
        val tvStatus = view.findViewById<TextView>(R.id.tv_notif_status)
        thread {
            var items: List<org.json.JSONObject> = emptyList()
            var unread = 0
            var err = ""
            try {
                val json = Api.post("notifications.php", mapOf("action" to "list"), savedToken)
                if (json.optBoolean("ok")) {
                    unread = json.optInt("unread", 0)
                    val arr = json.optJSONArray("items")
                    for (i in 0 until (arr?.length() ?: 0)) items += arr!!.getJSONObject(i)
                } else err = json.optString("error", "Ошибка")
            } catch (e: Exception) {
                err = "Нет связи с сервером"
            }
            activity?.runOnUiThread {
                listHolder.removeAllViews()
                if (err.isNotEmpty()) {
                    tvStatus.text = err
                    return@runOnUiThread
                }
                tvStatus.text = if (unread > 0) "Непрочитанных: " + unread else ""
                if (items.isEmpty()) {
                    val box = cardBox()
                    val big = TextView(requireContext())
                    big.text = "📭"
                    big.textSize = 40f
                    big.gravity = Gravity.CENTER
                    box.addView(big)
                    val t = TextView(requireContext())
                    t.text = "Уведомлений пока нет"
                    t.textSize = 15f
                    t.setTypeface(t.typeface, Typeface.BOLD)
                    t.gravity = Gravity.CENTER
                    t.setPadding(0, 16, 0, 8)
                    box.addView(t)
                    val sub = TextView(requireContext())
                    sub.text = "Здесь будут появляться важные события: начисления, модерация кампаний и сайтов, ответы поддержки."
                    sub.textSize = 13f
                    sub.setTextColor(Color.parseColor("#64748B"))
                    sub.gravity = Gravity.CENTER
                    box.addView(sub)
                    listHolder.addView(box)
                    return@runOnUiThread
                }
                for (n in items) listHolder.addView(card(view, n))
            }
        }
    }

    private fun card(view: View, n: org.json.JSONObject): View {
        val ctx = requireContext()
        val box = LinearLayout(ctx)
        box.orientation = LinearLayout.VERTICAL
        val bg = GradientDrawable().apply {
            setColor(if (n.optInt("is_read") == 1) Color.WHITE else Color.parseColor("#F0F9FF"))
            cornerRadius = 40f
        }
        box.background = bg
        box.setPadding(40, 36, 40, 36)
        box.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 20 }

        val head = LinearLayout(ctx)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL

        val ico = TextView(ctx)
        ico.text = n.optString("icon", "🔔")
        ico.textSize = 22f
        ico.setPadding(0, 0, 20, 0)
        head.addView(ico)

        val mid = LinearLayout(ctx)
        mid.orientation = LinearLayout.VERTICAL
        mid.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val title = TextView(ctx)
        title.text = n.optString("title")
        title.textSize = 15f
        title.setTypeface(title.typeface, Typeface.BOLD)
        title.setTextColor(if (n.optInt("is_read") == 1) Color.parseColor("#1E293B") else Color.parseColor("#2563eb"))
        mid.addView(title)

        val date = TextView(ctx)
        date.text = n.optString("date")
        date.textSize = 11f
        date.setTextColor(Color.parseColor("#64748B"))
        head.addView(date)

        box.addView(head)

        val msg = TextView(ctx)
        msg.text = n.optString("message")
        msg.textSize = 13f
        msg.setTextColor(Color.parseColor("#334155"))
        msg.setPadding(0, 10, 0, 12)
        box.addView(msg)

        val btns = LinearLayout(ctx)
        btns.orientation = LinearLayout.HORIZONTAL

        val link = n.optString("link")
        if (link.isNotEmpty()) {
            val go = TextView(ctx)
            go.text = "Перейти →"
            go.textSize = 13f
            go.setTextColor(Color.parseColor("#2563eb"))
            go.setPadding(0, 8, 32, 8)
            go.setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                } catch (_: Exception) {}
                markRead(view, n)
            }
            btns.addView(go)
        }

        if (n.optInt("is_read") == 0) {
            val read = TextView(ctx)
            read.text = "✓ Прочитано"
            read.textSize = 13f
            read.setTextColor(Color.parseColor("#64748B"))
            read.setPadding(0, 8, 32, 8)
            read.setOnClickListener { markRead(view, n) }
            btns.addView(read)
        }

        val del = TextView(ctx)
        del.text = "🗑 Удалить"
        del.textSize = 13f
        del.setTextColor(Color.parseColor("#EF4444"))
        del.setPadding(0, 8, 0, 8)
        del.setOnClickListener {
            thread {
                try {
                    Api.post("notifications.php", mapOf("action" to "delete", "id" to n.optInt("id").toString()), savedToken)
                    activity?.runOnUiThread { load(view) }
                } catch (_: Exception) {}
            }
        }
        btns.addView(del)

        box.addView(btns)
        return box
    }

    private fun markRead(view: View, n: org.json.JSONObject) {
        thread {
            try {
                Api.post("notifications.php", mapOf("action" to "mark_read", "id" to n.optInt("id").toString()), savedToken)
                activity?.runOnUiThread { load(view) }
            } catch (_: Exception) {}
        }
    }

    private fun cardBox(): LinearLayout {
        val box = LinearLayout(requireContext())
        box.orientation = LinearLayout.VERTICAL
        box.background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 40f }
        box.setPadding(40, 36, 40, 36)
        box.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 20 }
        return box
    }
}

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
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class DashboardFragment : Fragment() {

    private var badge: TextView? = null
    private var tvEarn: TextView? = null
    private var tvAdv: TextView? = null
    private var tvBonus: TextView? = null
    private var tvCamp: TextView? = null
    private var tvActive: TextView? = null
    private var tvRef: TextView? = null
    private var tvLevelTitle: TextView? = null
    private var tvHold: TextView? = null
    private var tvLevelDesc: TextView? = null
    private var tvStatus: TextView? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun roundBg(color: String, radiusDp: Int, strokeColor: String? = null, strokeDp: Int = 0): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(Color.parseColor(color))
        g.cornerRadius = dp(radiusDp).toFloat()
        if (strokeColor != null && strokeDp > 0) g.setStroke(dp(strokeDp), Color.parseColor(strokeColor))
        return g
    }

    private fun gradientBg(colors: Array<String>, radiusDp: Int): GradientDrawable {
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors.map { Color.parseColor(it) }.toIntArray())
        g.cornerRadius = dp(radiusDp).toFloat()
        return g
    }

    private fun cardView(ctx: Context, topMarginDp: Int = 10): LinearLayout {
        val c = LinearLayout(ctx)
        c.orientation = LinearLayout.VERTICAL
        c.background = roundBg("#ffffff", 12, "#e2e8f0", 1)
        c.setPadding(dp(14), dp(12), dp(14), dp(12))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(topMarginDp)
        c.layoutParams = lp
        return c
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()

        val scroll = ScrollView(ctx)
        scroll.setBackgroundColor(Color.parseColor("#f8fafc"))
        val content = LinearLayout(ctx)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(14), dp(14), dp(14), dp(24))
        scroll.addView(content)

        // ── Шапка: заголовок + колокольчик с бейджем ──
        val headRow = LinearLayout(ctx)
        headRow.orientation = LinearLayout.HORIZONTAL
        headRow.gravity = Gravity.CENTER_VERTICAL
        val title = TextView(ctx)
        title.text = "🏠 Главная"
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.parseColor("#1e293b"))
        title.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        headRow.addView(title)

        val bell = LinearLayout(ctx)
        bell.orientation = LinearLayout.HORIZONTAL
        bell.gravity = Gravity.CENTER
        bell.setPadding(dp(8), dp(6), dp(8), dp(6))
        bell.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }
        val bellIcon = TextView(ctx)
        bellIcon.text = "🔔"
        bellIcon.textSize = 20f
        bell.addView(bellIcon)

        val badgeTv = TextView(ctx)
        badgeTv.textSize = 10f
        badgeTv.setTypeface(null, Typeface.BOLD)
        badgeTv.setTextColor(Color.WHITE)
        badgeTv.gravity = Gravity.CENTER
        badgeTv.background = roundBg("#ef4444", 9)
        badgeTv.minWidth = dp(16)
        badgeTv.setPadding(dp(4), 0, dp(4), 0)
        badgeTv.visibility = View.GONE
        val badgeLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        badgeLp.leftMargin = dp(4)
        badgeTv.layoutParams = badgeLp
        bell.addView(badgeTv)
        badge = badgeTv
        headRow.addView(bell)
        content.addView(headRow)

        // ── Блок балансов — как в бургер-меню сайта ──
        val balOuter = LinearLayout(ctx)
        balOuter.orientation = LinearLayout.VERTICAL
        balOuter.background = roundBg("#f0f9ff", 10, "#7dd3fc", 2)
        balOuter.setPadding(dp(8), dp(8), dp(8), dp(8))
        val balOuterLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        balOuterLp.topMargin = dp(12)
        balOuter.layoutParams = balOuterLp

        val balInner = LinearLayout(ctx)
        balInner.orientation = LinearLayout.VERTICAL
        balInner.background = gradientBg(arrayOf("#38bdf8", "#0ea5e9", "#0284c7"), 10)
        balInner.setPadding(dp(12), dp(10), dp(12), dp(10))
        balOuter.addView(balInner)

        fun addBalRow(label: String, sizeSp: Float): TextView {
            val row = LinearLayout(ctx)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val lab = TextView(ctx)
            lab.text = label
            lab.textSize = 10f
            lab.setTypeface(null, Typeface.BOLD)
            lab.setTextColor(Color.parseColor("#e0f2fe"))
            row.addView(lab)
            val space = View(ctx)
            space.layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            row.addView(space)
            val v = TextView(ctx)
            v.text = "—"
            v.textSize = sizeSp
            v.setTextColor(Color.WHITE)
            row.addView(v)
            balInner.addView(row)
            return v
        }

        tvEarn = addBalRow("💰 Заработок", 15f)

        val div = View(ctx)
        div.setBackgroundColor(Color.parseColor("#40ffffff"))
        val divLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        divLp.setMargins(0, dp(6), 0, dp(6))
        div.layoutParams = divLp
        balInner.addView(div)

        tvAdv = addBalRow("📢 Рекламный", 13f)

        val bonus = TextView(ctx)
        bonus.textSize = 10.5f
        bonus.setTextColor(Color.parseColor("#f0f9ff"))
        bonus.gravity = Gravity.CENTER
        bonus.visibility = View.GONE
        val bonusLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        bonusLp.topMargin = dp(6)
        bonus.layoutParams = bonusLp
        balInner.addView(bonus)
        tvBonus = bonus

        content.addView(balOuter)

        // ── Три счётчика: кампании, активные, рефералы ──
        val statsRow = LinearLayout(ctx)
        statsRow.orientation = LinearLayout.HORIZONTAL
        val statsLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        statsLp.topMargin = dp(10)
        statsRow.layoutParams = statsLp

        fun statCard(numColor: String, label: String): TextView {
            val c = LinearLayout(ctx)
            c.orientation = LinearLayout.VERTICAL
            c.gravity = Gravity.CENTER
            c.background = roundBg("#ffffff", 12, "#e2e8f0", 1)
            c.setPadding(dp(6), dp(10), dp(6), dp(10))
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.leftMargin = dp(3)
            lp.rightMargin = dp(3)
            c.layoutParams = lp
            val num = TextView(ctx)
            num.textSize = 16f
            num.setTypeface(null, Typeface.BOLD)
            num.setTextColor(Color.parseColor(numColor))
            num.text = "—"
            num.gravity = Gravity.CENTER
            c.addView(num)
            val lab = TextView(ctx)
            lab.text = label
            lab.textSize = 10f
            lab.setTextColor(Color.parseColor("#64748b"))
            lab.gravity = Gravity.CENTER
            c.addView(lab)
            statsRow.addView(c)
            return num
        }

        tvCamp = statCard("#3498db", "📢 Кампаний")
        tvActive = statCard("#10b981", "✅ Активных")
        tvRef = statCard("#3498db", "🎁 Рефералов")
        content.addView(statsRow)

        // ── Карточка уровня (как в кабинете на сайте) ──
        val levelCard = LinearLayout(ctx)
        levelCard.orientation = LinearLayout.HORIZONTAL
        levelCard.background = roundBg("#ffffff", 12, "#e2e8f0", 1)
        val levelLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        levelLp.topMargin = dp(10)
        levelCard.layoutParams = levelLp

        val stripe = View(ctx)
        stripe.background = roundBg("#3498db", 0)
        stripe.layoutParams = LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT)
        levelCard.addView(stripe)

        val lInner = LinearLayout(ctx)
        lInner.orientation = LinearLayout.VERTICAL
        lInner.setPadding(dp(12), dp(10), dp(12), dp(10))

        val lvlTitle = TextView(ctx)
        lvlTitle.text = "⭐ Ваш уровень: —"
        lvlTitle.textSize = 15f
        lvlTitle.setTypeface(null, Typeface.BOLD)
        lvlTitle.setTextColor(Color.parseColor("#1e293b"))
        lInner.addView(lvlTitle)
        tvLevelTitle = lvlTitle

        val hold = TextView(ctx)
        hold.textSize = 13f
        hold.setTextColor(Color.parseColor("#1e293b"))
        hold.visibility = View.GONE
        lInner.addView(hold)
        tvHold = hold

        val lvlDesc = TextView(ctx)
        lvlDesc.textSize = 12f
        lvlDesc.setTextColor(Color.parseColor("#64748b"))
        lInner.addView(lvlDesc)
        tvLevelDesc = lvlDesc

        levelCard.addView(lInner)
        content.addView(levelCard)

        // ── Быстрые действия ──
        val qa = cardView(ctx)
        val qaTitle = TextView(ctx)
        qaTitle.text = "⚡ Быстрые действия"
        qaTitle.textSize = 16f
        qaTitle.setTypeface(null, Typeface.BOLD)
        qaTitle.setTextColor(Color.parseColor("#1e293b"))
        qa.addView(qaTitle)

        fun openSite(path: String) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://adlinkpro.ru/" + path)))
            } catch (_: Exception) {}
        }

        fun actBtn(label: String, onClick: () -> Unit): TextView {
            val b = TextView(ctx)
            b.text = label
            b.textSize = 13f
            b.setTypeface(null, Typeface.BOLD)
            b.setTextColor(Color.WHITE)
            b.gravity = Gravity.CENTER
            b.background = roundBg("#3498db", 8)
            b.setPadding(dp(10), dp(12), dp(10), dp(12))
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(dp(4), dp(4), dp(4), dp(4))
            b.layoutParams = lp
            b.setOnClickListener { onClick() }
            return b
        }

        fun actRow(vararg bs: TextView) {
            val row = LinearLayout(ctx)
            row.orientation = LinearLayout.HORIZONTAL
            for (b in bs) row.addView(b)
            qa.addView(row)
        }

        actRow(
            actBtn("💰 Пополнить") { openSite("user/balance.php") },
            actBtn("📢 Создать кампанию") { openSite("user/campaign_add.php") }
        )
        actRow(
            actBtn("🌐 Витрина") { openSite("user/showcase.php") },
            actBtn("📦 Тираж") { startActivity(Intent(requireContext(), CpmOrderActivity::class.java)) }
        )
        actRow(
            actBtn("🎁 Партнёрам") { startActivity(Intent(requireContext(), ReferralActivity::class.java)) },
            actBtn("💸 Вывести") { openSite("user/withdraw.php") }
        )
        content.addView(qa)

        // ── Строка состояния (ошибки загрузки) ──
        val status = TextView(ctx)
        status.textSize = 13f
        status.setTextColor(Color.parseColor("#b91c1c"))
        status.gravity = Gravity.CENTER
        status.visibility = View.GONE
        val statusLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        statusLp.topMargin = dp(10)
        status.layoutParams = statusLp
        content.addView(status)
        tvStatus = status

        return scroll
    }

    override fun onResume() {
        super.onResume()
        loadNotifCount()
        loadHome()
    }

    private fun loadHome() {
        val sp = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        val token = sp.getString("token", null) ?: return
        thread {
            try {
                val json = Api.post("home.php", emptyMap(), token)
                if (json.optBoolean("ok")) {
                    activity?.runOnUiThread {
                        tvEarn?.text = json.optString("balance", "0,00 ₽")
                        tvAdv?.text = json.optString("ad_balance", "0,00 ₽")
                        val note = json.optString("bonus_note", "")
                        if (note.isNotEmpty()) {
                            tvBonus?.visibility = View.VISIBLE
                            tvBonus?.text = note
                        } else {
                            tvBonus?.visibility = View.GONE
                        }
                        tvCamp?.text = json.optString("campaigns", "0")
                        tvActive?.text = json.optString("active", "0")
                        tvRef?.text = json.optString("referrals", "0")
                        tvLevelTitle?.text = "⭐ Ваш уровень: " + json.optString("level_label", "Уровень 1")
                        val holdRaw = json.optDouble("hold_raw", 0.0)
                        if (holdRaw > 0) {
                            tvHold?.visibility = View.VISIBLE
                            tvHold?.text = "🔒 В холде: " + json.optString("hold", "0,00 ₽") +
                                    " — доход за свежие показы. Выпустится через " + json.optString("hold_hours", "24") + " ч."
                        } else {
                            tvHold?.visibility = View.GONE
                        }
                        tvLevelDesc?.text = json.optString("level_desc", "")
                        tvStatus?.visibility = View.GONE
                    }
                } else {
                    activity?.runOnUiThread {
                        tvStatus?.visibility = View.VISIBLE
                        tvStatus?.text = "Не удалось загрузить: " + json.optString("error", "")
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    tvStatus?.visibility = View.VISIBLE
                    tvStatus?.text = "Нет связи с сервером"
                }
            }
        }
    }

    private fun loadNotifCount() {
        val sp = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        val token = sp.getString("token", null) ?: return
        val b = badge ?: return
        thread {
            try {
                val json = Api.post("notifications.php", mapOf("action" to "count"), token)
                if (!json.optBoolean("ok")) return@thread
                val unread = json.optInt("unread", 0)
                activity?.runOnUiThread {
                    if (unread > 0) {
                        b.visibility = View.VISIBLE
                        b.text = if (unread > 99) "99+" else unread.toString()
                    } else {
                        b.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {}
        }
    }
}

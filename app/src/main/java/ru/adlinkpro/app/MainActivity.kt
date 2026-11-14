package ru.adlinkpro.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val activeColor = Color.parseColor("#3498db")
    private val idleColor = Color.parseColor("#94a3b8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setupNavPanel()

        findViewById<View>(R.id.nav_home).setOnClickListener {
            openTab(R.id.nav_home) { DashboardFragment() }
        }
        findViewById<View>(R.id.nav_news).setOnClickListener {
            openTab(R.id.nav_news) { NewsFragment() }
        }
        findViewById<View>(R.id.nav_wallet).setOnClickListener {
            openTab(R.id.nav_wallet) {
                WalletFragment().apply {
                    arguments = Bundle().apply { putString("section", "deposit") }
                }
            }
        }
        findViewById<View>(R.id.nav_more).setOnClickListener {
            openTab(R.id.nav_more) { MoreFragment() }
        }

        if (savedInstanceState == null) {
            openTab(R.id.nav_home) { DashboardFragment() }
        }

        refreshNotifBadge()

        /* fw-push: свои пуши — запускаем быстрый опрос и страховку */
        requestNotifPermission()
        PushHelper.ensureChannel(this)
        try {
            ContextCompat.startForegroundService(this, Intent(this, PollService::class.java))
        } catch (_: Exception) {}
        PushHelper.scheduleFallback(this)

        /* fw-push: тап по уведомлению открывает раздел уведомлений */
        if (savedInstanceState == null && intent?.getBooleanExtra("open_notifications", false) == true) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7001)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    /** Тёмная капсула и круглая центральная кнопка со светящейся обводкой */
    private fun setupNavPanel() {
        val panel = findViewById<LinearLayout>(R.id.nav_panel)
        val capsule = GradientDrawable()
        capsule.setColor(Color.parseColor("#0f172a"))
        capsule.cornerRadius = dp(34).toFloat()
        panel.background = capsule
        panel.setPadding(dp(6), dp(8), dp(6), dp(8))

        val walletBtn = findViewById<FrameLayout>(R.id.nav_wallet_btn)
        val ring = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#38bdf8"), Color.parseColor("#0284c7"))
        )
        ring.cornerRadius = dp(23).toFloat()
        ring.setStroke(dp(2), Color.parseColor("#7dd3fc"))
        walletBtn.background = ring
    }

    private fun openTab(tabId: Int, factory: () -> Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, factory())
            .commit()
        highlight(tabId)
    }

    /** Подсветка активного пункта: подложка + подпись синим, как на референсе */
    private fun highlight(tabId: Int) {
        val slots = intArrayOf(R.id.nav_home, R.id.nav_news, R.id.nav_wallet, R.id.nav_more)
        for (id in slots) {
            val slot = findViewById<LinearLayout>(id) ?: continue
            val icon = slot.getChildAt(0) as? TextView
            val label = slot.getChildAt(1) as? TextView
            val on = id == tabId
            val color = if (on) activeColor else idleColor

            if (on) {
                val bg = GradientDrawable()
                bg.setColor(Color.parseColor("#243450"))
                bg.cornerRadius = dp(14).toFloat()
                slot.background = bg
            } else {
                slot.background = null
            }
            label?.setTextColor(color)
            label?.setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    fun refreshNotifBadge() {
        val sp = getSharedPreferences("app", MODE_PRIVATE)
        val token = sp.getString("token", null) ?: return
        val badge = findViewById<TextView>(R.id.nav_more_badge)
        thread {
            try {
                val json = Api.post("notifications.php", mapOf("action" to "count"), token)
                if (!json.optBoolean("ok")) return@thread
                val unread = json.optInt("unread", 0)
                runOnUiThread {
                    if (unread > 0) {
                        badge.visibility = View.VISIBLE
                        badge.text = if (unread > 99) "99+" else unread.toString()
                    } else {
                        badge.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {}
        }
    }
}

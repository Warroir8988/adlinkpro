package ru.adlinkpro.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sp = getSharedPreferences("app", MODE_PRIVATE)
        val token = sp.getString("token", null)
        val pin = sp.getString("pin", null)

        when {
            // Нет токена — на вход
            token == null -> {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }
            // Токен есть, пина нет — LoginActivity сам предложит придумать пин
            pin == null -> {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }
            // Токен и пин есть: если пин ещё не подтверждён в этом запуске — на ввод пина
            !intent.getBooleanExtra("pin_ok", false) -> {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }
        }

        setContentView(R.layout.activity_main)
        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            val f: Fragment = when (item.itemId) {
                R.id.nav_home -> DashboardFragment()
                R.id.nav_news -> NewsFragment()
                else -> MoreFragment()
            }
            supportFragmentManager.beginTransaction().replace(R.id.container, f).commit()
            true
        }
        if (savedInstanceState == null) nav.selectedItemId = R.id.nav_home

        refreshNotifBadge()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем счётчик при возвращении на экран — например, после просмотра уведомлений
        refreshNotifBadge()
    }

    private fun refreshNotifBadge() {
        val sp = getSharedPreferences("app", MODE_PRIVATE)
        val token = sp.getString("token", null) ?: return
        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        thread {
            try {
                val json = Api.post("notifications.php", mapOf("action" to "count"), token)
                if (!json.optBoolean("ok")) return@thread
                val unread = json.optInt("unread", 0)
                runOnUiThread {
                    // Третий пункт меню — «Ещё», на него вешаем колокольчик с цифрой
                    val moreItem = nav.menu.getItem(2)
                    val badge = nav.getOrCreateBadge(moreItem.itemId)
                    if (unread > 0) {
                        badge.isVisible = true
                        badge.number = unread
                    } else {
                        badge.isVisible = false
                    }
                }
            } catch (_: Exception) {}
        }
    }
}

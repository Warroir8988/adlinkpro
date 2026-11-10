package ru.adlinkpro.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

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
    }
}

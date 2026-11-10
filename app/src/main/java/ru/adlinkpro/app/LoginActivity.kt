package ru.adlinkpro.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sp = getSharedPreferences("app", Context.MODE_PRIVATE)
        if (sp.getString("token", null) != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_login)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnRegister = findViewById<Button>(R.id.btn_register)
        val tvStatus = findViewById<TextView>(R.id.tv_status)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            if (email.isEmpty() || pass.isEmpty()) {
                tvStatus.text = "Заполните оба поля"
                return@setOnClickListener
            }
            tvStatus.text = "Входим…"
            btnLogin.isEnabled = false
            thread {
                try {
                    val json = Api.post(
                        "auth.php",
                        mapOf("action" to "login", "email" to email, "password" to pass),
                        null
                    )
                    if (json.optBoolean("ok")) {
                        sp.edit().putString("token", json.optString("token")).apply()
                        runOnUiThread {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            tvStatus.text = json.optString("error", "Неверная почта или пароль")
                            btnLogin.isEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        tvStatus.text = "Нет связи с сервером"
                        btnLogin.isEnabled = true
                    }
                }
            }
        }

        btnRegister.setOnClickListener {
            tvStatus.text = "Регистрация скоро заработает"
        }
    }
}

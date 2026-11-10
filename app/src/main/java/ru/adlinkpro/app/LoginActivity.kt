package ru.adlinkpro.app

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
        if (getSharedPreferences("app", MODE_PRIVATE).getString("token", null) != null) {
            goMain(); return
        }
        setContentView(R.layout.activity_login)
        val email = findViewById<EditText>(R.id.et_email)
        val pass = findViewById<EditText>(R.id.et_password)
        val status = findViewById<TextView>(R.id.tv_status)

        fun send(action: String) {
            if (email.text.isBlank() || pass.text.isBlank()) {
                status.text = "Заполните оба поля"; return
            }
            status.text = "Подождите…"
            thread {
                try {
                    val json = Api.post(
                        "auth.php",
                        mapOf("action" to action, "email" to email.text.toString().trim(), "password" to pass.text.toString()),
                        null
                    )
                    if (json.optBoolean("ok")) {
                        getSharedPreferences("app", MODE_PRIVATE)
                            .edit().putString("token", json.getString("token")).apply()
                        runOnUiThread { goMain() }
                    } else {
                        runOnUiThread { status.text = json.optString("error", "Ошибка. Попробуйте ещё раз") }
                    }
                } catch (e: Exception) {
                    runOnUiThread { status.text = "Нет связи с сервером. Проверьте интернет" }
                }
            }
        }
        findViewById<Button>(R.id.btn_login).setOnClickListener { send("login") }
        findViewById<Button>(R.id.btn_register).setOnClickListener { send("register") }
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

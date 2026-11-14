package ru.adlinkpro.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.security.MessageDigest
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    private lateinit var sp: SharedPreferences
    private var setupFirst = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sp = getSharedPreferences("app", Context.MODE_PRIVATE)
        val token = sp.getString("token", null)
        val pin = sp.getString("pin", null)
        when {
            token == null -> showLogin()
            pin == null -> showPin(false)
            else -> showPin(true)
        }
    }

    private fun goMain() {
        val i = Intent(this, MainActivity::class.java)
        i.putExtra("pin_ok", true)
        startActivity(i)
        finish()
    }

    private fun showLogin() {
        setupFirst = ""
        setContentView(R.layout.activity_login)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnRegister = findViewById<Button>(R.id.btn_register)
        val tvForgot = findViewById<TextView>(R.id.tv_forgot)
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
                            if (sp.getString("pin", null) == null) showPin(false) else goMain()
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
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgot.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun showPin(enter: Boolean) {
        setContentView(R.layout.activity_pin)
        val tvTitle = findViewById<TextView>(R.id.tv_pin_title)
        val tvStatus = findViewById<TextView>(R.id.tv_pin_status)
        val btnForgot = findViewById<Button>(R.id.btn_forgot)
        val dots = listOf(
            findViewById<TextView>(R.id.dot1),
            findViewById<TextView>(R.id.dot2),
            findViewById<TextView>(R.id.dot3),
            findViewById<TextView>(R.id.dot4)
        )
        val et = findViewById<EditText>(R.id.et_pin)

        fun render(len: Int) {
            tvTitle.text = when {
                enter -> "Введите PIN"
                setupFirst.isEmpty() -> "Придумайте PIN"
                else -> "Повторите PIN"
            }
            dots.forEachIndexed { i, d -> d.text = if (i < len) "●" else "○" }
        }
        render(0)

        et.filters = arrayOf(InputFilter.LengthFilter(4))
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val len = s?.length ?: 0
                render(len)
                tvStatus.text = ""
                if (len < 4) return
                val code = s.toString()
                et.setText("")
                if (enter) {
                    if (sha256(code) == sp.getString("pin", "")) {
                        goMain()
                    } else {
                        render(0)
                        tvStatus.text = "Неверный PIN"
                    }
                } else {
                    if (setupFirst.isEmpty()) {
                        setupFirst = code
                        render(0)
                    } else if (code == setupFirst) {
                        sp.edit().putString("pin", sha256(code)).apply()
                        goMain()
                    } else {
                        setupFirst = ""
                        render(0)
                        tvStatus.text = "PIN-коды не совпали, начнём заново"
                    }
                }
            }
        })

        btnForgot.visibility = if (enter) View.VISIBLE else View.GONE
        btnForgot.setOnClickListener {
            sp.edit().remove("pin").apply()
            showLogin()
        }

        et.requestFocus()
        et.post {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

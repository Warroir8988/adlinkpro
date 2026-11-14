package ru.adlinkpro.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.concurrent.thread

class ForgotPasswordActivity : Activity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private lateinit var status: TextView
    private lateinit var inEmail: EditText
    private lateinit var btn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor("#f8fafc"))
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(20), dp(28), dp(20), dp(28))
        scroll.addView(content)

        val logo = ImageView(this)
        logo.setImageResource(R.drawable.logo)
        val logoLp = LinearLayout.LayoutParams(dp(84), dp(84))
        logoLp.gravity = Gravity.CENTER_HORIZONTAL
        logo.layoutParams = logoLp
        content.addView(logo)

        val title = TextView(this)
        title.text = "Восстановление пароля"
        title.textSize = 20f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(Color.parseColor("#1e293b"))
        title.gravity = Gravity.CENTER
        val titleLp = LinearLayout.LayoutParams(-1, -2)
        titleLp.topMargin = dp(16)
        title.layoutParams = titleLp
        content.addView(title)

        val sub = TextView(this)
        sub.text = "Введите email — отправим одноразовую ссылку для смены пароля"
        sub.textSize = 13f
        sub.setTextColor(Color.parseColor("#64748b"))
        sub.gravity = Gravity.CENTER
        content.addView(sub)

        content.addView(label("Email"))
        inEmail = input("name@example.com")
        content.addView(inEmail)

        btn = button("Отправить ссылку")
        val btnLp = LinearLayout.LayoutParams(-1, -2)
        btnLp.topMargin = dp(18)
        btn.layoutParams = btnLp
        btn.setOnClickListener { submit() }
        content.addView(btn)

        status = TextView(this)
        status.textSize = 13f
        status.setTextColor(Color.parseColor("#334155"))
        status.visibility = View.GONE
        status.gravity = Gravity.CENTER
        val stLp = LinearLayout.LayoutParams(-1, -2)
        stLp.topMargin = dp(14)
        status.layoutParams = stLp
        content.addView(status)

        setContentView(scroll)
    }

    private fun label(text: String): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = 13f
        t.setTypeface(null, Typeface.BOLD)
        t.setTextColor(Color.parseColor("#475569"))
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.topMargin = dp(14)
        lp.bottomMargin = dp(6)
        t.layoutParams = lp
        return t
    }

    private fun input(hint: String): EditText {
        val e = EditText(this)
        e.hint = hint
        e.textSize = 14f
        e.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        e.setTextColor(Color.parseColor("#1e293b"))
        e.setHintTextColor(Color.parseColor("#94a3b8"))
        val bg = GradientDrawable()
        bg.setColor(Color.parseColor("#f1f5f9"))
        bg.cornerRadius = dp(10).toFloat()
        bg.setStroke(dp(1), Color.parseColor("#e2e8f0"))
        e.background = bg
        e.setPadding(dp(12), dp(11), dp(12), dp(11))
        e.setSingleLine(true)
        return e
    }

    private fun button(text: String): TextView {
        val b = TextView(this)
        b.text = text
        b.textSize = 15f
        b.setTypeface(null, Typeface.BOLD)
        b.setTextColor(Color.WHITE)
        b.gravity = Gravity.CENTER
        val bg = GradientDrawable()
        bg.setColor(Color.parseColor("#3498db"))
        bg.cornerRadius = dp(12).toFloat()
        b.background = bg
        b.setPadding(0, dp(13), 0, dp(13))
        return b
    }

    private fun submit() {
        val emailStr = inEmail.text.toString().trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) {
            status.visibility = View.VISIBLE
            status.setTextColor(Color.parseColor("#b91c1c"))
            status.text = "Введите корректный email"
            return
        }

        btn.isEnabled = false
        btn.text = "Отправляем..."
        btn.alpha = 0.6f

        thread {
            var err = ""
            var okMsg = ""
            try {
                val json = Api.post("auth.php", mapOf(
                    "action" to "forgot",
                    "email" to emailStr
                ), null)
                if (json.optBoolean("ok")) okMsg = json.optString("message", "Письмо отправлено")
                else err = json.optString("error", "Ошибка")
            } catch (e: Exception) {
                err = "Нет связи с сервером"
            }
            runOnUiThread {
                btn.alpha = 1f
                if (err.isNotEmpty()) {
                    btn.isEnabled = true
                    btn.text = "Отправить ссылку"
                    status.visibility = View.VISIBLE
                    status.setTextColor(Color.parseColor("#b91c1c"))
                    status.text = err
                    return@runOnUiThread
                }
                status.visibility = View.VISIBLE
                status.setTextColor(Color.parseColor("#065f46"))
                status.text = okMsg
            }
        }
    }
}

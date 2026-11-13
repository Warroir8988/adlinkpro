package ru.adlinkpro.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

object Ui {
    val BG = Color.parseColor("#f1f5f9")
    val CARD = Color.WHITE
    val BORDER = Color.parseColor("#e2e8f0")
    val TEXT = Color.parseColor("#1e293b")
    val MUTED = Color.parseColor("#64748b")
    val PRIMARY = Color.parseColor("#3498db")
    val SUCCESS = Color.parseColor("#059669")
    val WARNING = Color.parseColor("#f59e0b")
    val DANGER = Color.parseColor("#dc2626")

    fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    fun card(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            cornerRadius = dp(ctx, 6).toFloat()
            setColor(CARD)
            setStroke(dp(ctx, 1), BORDER)
        }
        setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(ctx, 14)) }
    }

    fun title(ctx: Context, text: String, size: Float = 15f): TextView = TextView(ctx).apply {
        this.text = text
        textSize = size
        setTypeface(null, Typeface.BOLD)
        setTextColor(TEXT)
        setPadding(0, 0, 0, dp(ctx, 10))
    }

    fun muted(ctx: Context, text: String, padV: Int = 20): TextView = TextView(ctx).apply {
        this.text = text
        textSize = 13f
        setTextColor(MUTED)
        gravity = Gravity.CENTER
        setPadding(0, dp(ctx, padV), 0, dp(ctx, padV))
    }

    fun codeBox(ctx: Context, text: String): TextView = TextView(ctx).apply {
        this.text = text
        textSize = 12f
        setTextColor(TEXT)
        setTypeface(Typeface.MONOSPACE)
        background = GradientDrawable().apply {
            cornerRadius = dp(ctx, 6).toFloat()
            setColor(Color.parseColor("#f8fafc"))
            setStroke(dp(ctx, 1), BORDER)
        }
        setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8))
    }

    fun btnPrimary(ctx: Context, text: String, onClick: () -> Unit): TextView = TextView(ctx).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setTypeface(null, Typeface.BOLD)
        background = GradientDrawable().apply { cornerRadius = dp(ctx, 6).toFloat(); setColor(PRIMARY) }
        setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10))
        setOnClickListener { onClick() }
    }

    fun btnGhost(ctx: Context, text: String, onClick: () -> Unit): TextView = TextView(ctx).apply {
        this.text = text
        textSize = 13f
        setTextColor(TEXT)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply { cornerRadius = dp(ctx, 6).toFloat(); setColor(CARD); setStroke(dp(ctx, 1), BORDER) }
        setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8))
        setOnClickListener { onClick() }
    }

    fun statTile(ctx: Context, value: String, label: String, color: Int = TEXT): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        background = GradientDrawable().apply { cornerRadius = dp(ctx, 6).toFloat(); setColor(CARD); setStroke(dp(ctx, 1), BORDER) }
        setPadding(dp(ctx, 8), dp(ctx, 10), dp(ctx, 8), dp(ctx, 10))
        addView(TextView(ctx).apply { text = value; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(color); gravity = Gravity.CENTER })
        addView(TextView(ctx).apply { text = label; textSize = 11f; setTextColor(MUTED); gravity = Gravity.CENTER; setPadding(0, dp(ctx, 3), 0, 0) })
    }
}

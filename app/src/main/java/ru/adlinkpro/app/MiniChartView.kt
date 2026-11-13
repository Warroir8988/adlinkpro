package ru.adlinkpro.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class MiniChartView(context: Context) : View(context) {

    // Точки: (подпись, значение первой серии, значение второй серии)
    var data: List<Triple<String, Float, Float>> = emptyList()
    var seriesColors: List<Int> = listOf(Color.parseColor("#4f46e5"), Color.parseColor("#10b981"))
    var seriesNames: List<String> = listOf("Показы", "Клики")

    private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748b")
        textSize = 11f * resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val legendY = 30f * d

        var lx = 0f
        for (i in seriesNames.indices) {
            bar.color = seriesColors[i]
            canvas.drawRect(lx, legendY - 16f * d, lx + 18f * d, legendY - 4f * d, bar)
            txt.textAlign = Paint.Align.LEFT
            canvas.drawText(seriesNames[i], lx + 22f * d, legendY, txt)
            lx += 22f * d + txt.measureText(seriesNames[i]) + 28f * d
        }

        if (data.isEmpty()) return
        val n = data.size
        val h = height.toFloat()
        val w = width.toFloat()
        val maxV = data.flatMap { listOf(it.second, it.third) }.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val bottom = h - 30f * d
        val gap = w * 0.03f
        val groupW = (w - gap * (n + 1)) / n
        val barW = groupW * (if (seriesNames.size > 1) 0.42f else 0.85f)
        var x = gap
        val labelStep = if (n > 8) (n + 7) / 8 else 1
        for ((idx, item) in data.withIndex()) {
            var bx = x + (groupW - barW * seriesNames.size - gap * (seriesNames.size - 1)) / 2f
            val values = listOf(item.second, item.third)
            for (s in seriesNames.indices) {
                val v = values[s]
                if (v > 0f) {
                    bar.color = seriesColors[s]
                    val bh = (v / maxV) * (bottom - legendY - 6f * d)
                    canvas.drawRoundRect(bx, bottom - bh, bx + barW, bottom, 4f * d, 4f * d, bar)
                }
                bx += barW + gap
            }
            if (idx % labelStep == 0) {
                txt.textAlign = Paint.Align.CENTER
                canvas.drawText(item.first, x + groupW / 2, h - 8f * d, txt)
            }
            x += groupW + gap
        }
    }
}

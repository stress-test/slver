package com.example.bbsolver

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class OverlayView(ctx: Context) : View(ctx) {
    var moves: List<Move>? = null
    var cfg: Map<String, Float>? = null
    var msg = ""
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cols = intArrayOf(0xCC2F80ED.toInt(), 0xCC27AE60.toInt(), 0xCCF2994A.toInt())

    override fun onDraw(cv: Canvas) {
        val c = cfg ?: return
        val W = width.toFloat(); val H = height.toFloat()
        val cell = c["bs"]!! * W / 8; val x0 = c["bx"]!! * W; val y0 = c["by"]!! * H
        p.textAlign = Paint.Align.CENTER; p.textSize = cell * 0.55f
        for (i in 0 until 3) { p.color = cols[i]; cv.drawText("${i + 1}", W * (i + .5f) / 3, c["ty"]!! * H - 8f, p) }
        moves?.forEachIndexed { i, m ->
            p.color = cols[m.pi % 3]
            for (q in m.cells) {
                val l = x0 + (m.c + q[1]) * cell; val t = y0 + (m.r + q[0]) * cell
                cv.drawRect(l + 3, t + 3, l + cell - 3, t + cell - 3, p)
            }
            p.color = Color.WHITE
            val f = m.cells[0]
            cv.drawText("${i + 1}", x0 + (m.c + f[1] + .5f) * cell, y0 + (m.r + f[0] + .5f) * cell + p.textSize / 3, p)
        }
        p.color = 0xCC000000.toInt(); cv.drawRect(0f, 0f, W, cell, p)
        p.color = Color.WHITE; p.textSize = cell * 0.4f; cv.drawText(msg, W / 2, cell * 0.65f, p)
    }
}

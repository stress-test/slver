package com.example.bbsolver

import android.graphics.Bitmap
import android.graphics.Color
import android.content.Context

object Cfg {
    // Tất cả là tỉ lệ so với màn hình. bx,bs theo chiều rộng; by,ty,th theo chiều cao.
    val DEF = linkedMapOf("bx" to 0.04f, "by" to 0.28f, "bs" to 0.92f, "ty" to 0.74f, "th" to 0.20f, "ts" to 0.60f)
    val LABEL = mapOf(
        "bx" to "Bảng: lề trái / rộng màn hình", "by" to "Bảng: mép trên / cao màn hình",
        "bs" to "Bảng: cạnh bảng / rộng màn hình", "ty" to "Khay mảnh: mép trên / cao màn hình",
        "th" to "Khay mảnh: chiều cao / cao màn hình", "ts" to "Ô trong khay / ô trên bảng (kích thước)")
    fun load(c: Context): Map<String, Float> {
        val p = c.getSharedPreferences("cfg", 0)
        return DEF.mapValues { p.getFloat(it.key, it.value) }
    }
}

object Detector {
    private fun bright(p: Int): Boolean {
        val hsv = FloatArray(3); Color.colorToHSV(p, hsv)
        return hsv[2] > 0.6f && hsv[1] > 0.25f   // ô có khối: sáng + bão hòa; ô trống: tối
    }
    private fun px(b: Bitmap, x: Int, y: Int, W: Int, H: Int) = b.getPixel(x.coerceIn(0, W - 1), y.coerceIn(0, H - 1))

    fun board(b: Bitmap, W: Int, H: Int, c: Map<String, Float>): IntArray {
        val cell = c["bs"]!! * W / 8; val x0 = c["bx"]!! * W; val y0 = c["by"]!! * H
        return IntArray(8) { r ->
            var m = 0
            for (k in 0 until 8) if (bright(px(b, (x0 + (k + .5f) * cell).toInt(), (y0 + (r + .5f) * cell).toInt(), W, H))) m = m or (1 shl k)
            m
        }
    }

    fun pieces(b: Bitmap, W: Int, H: Int, c: Map<String, Float>): List<List<IntArray>> {
        val ideal = c["bs"]!! * W / 8 * c["ts"]!!
        val ty = (c["ty"]!! * H).toInt(); val th = (c["th"]!! * H).toInt()
        return (0 until 3).map { i ->
            val xa = W * i / 3; val xb = W * (i + 1) / 3
            var minX = Int.MAX_VALUE; var maxX = -1; var minY = Int.MAX_VALUE; var maxY = -1
            var y = ty
            while (y < ty + th) {
                var x = xa
                while (x < xb) {
                    if (bright(px(b, x, y, W, H))) { minX = minOf(minX, x); maxX = maxOf(maxX, x); minY = minOf(minY, y); maxY = maxOf(maxY, y) }
                    x += 4
                }
                y += 4
            }
            if (maxX < 0) return@map emptyList<IntArray>()
            val bw = (maxX - minX + 4).toFloat(); val bh = (maxY - minY + 4).toFloat()
            var bestS = 0f; var bestD = Float.MAX_VALUE
            for (k in 1..5) {
                val s = bw / k; val rr = Math.round(bh / s)
                if (rr < 1 || rr > 5 || Math.abs(bh - rr * s) > s * 0.3f) continue
                val d = Math.abs(s - ideal)
                if (d < bestD) { bestD = d; bestS = s }
            }
            if (bestS == 0f) return@map emptyList<IntArray>()
            val cols = Math.round(bw / bestS); val rows = Math.round(bh / bestS)
            val out = ArrayList<IntArray>()
            for (r in 0 until rows) for (k in 0 until cols)
                if (bright(px(b, (minX + (k + .5f) * bestS).toInt(), (minY + (r + .5f) * bestS).toInt(), W, H))) out.add(intArrayOf(r, k))
            out
        }
    }
}

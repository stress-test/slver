package com.example.bbsolver

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

class CaptureService : Service() {
    private lateinit var wm: WindowManager
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var vd: VirtualDisplay? = null
    private lateinit var overlay: OverlayView
    private lateinit var btn: TextView
    private val h = Handler(Looper.getMainLooper())
    private var W = 0; private var H = 0
    private var last: Bitmap? = null

    override fun onBind(i: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("bb", "BB Solver", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "bb").setContentTitle("Block Blast Solver đang chạy").setSmallIcon(android.R.drawable.ic_menu_search).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(1, n)

        val data = i!!.getParcelableExtra<Intent>("data")!!
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(i.getIntExtra("code", 0), data)
        projection!!.registerCallback(object : MediaProjection.Callback() { override fun onStop() { stopSelf() } }, h)

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics(); wm.defaultDisplay.getRealMetrics(dm)
        W = dm.widthPixels; H = dm.heightPixels
        reader = ImageReader.newInstance(W, H, PixelFormat.RGBA_8888, 2)
        vd = projection!!.createVirtualDisplay("bb", W, H, dm.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, null)

        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        overlay = OverlayView(this)
        wm.addView(overlay, WindowManager.LayoutParams(-1, -1, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT))
        btn = TextView(this).apply {
            text = "GIẢI"; textSize = 16f; setTextColor(Color.WHITE); setBackgroundColor(0xE0D9480F.toInt()); setPadding(32, 20, 32, 20)
            setOnClickListener { scan() }
            setOnLongClickListener { stopSelf(); true }   // nhấn giữ để tắt
        }
        wm.addView(btn, WindowManager.LayoutParams(-2, -2, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = 0; y = H / 2
        })
        return START_NOT_STICKY
    }

    private fun grab(): Bitmap? {
        val img = reader?.acquireLatestImage() ?: return null
        val pl = img.planes[0]
        val pad = pl.rowStride - pl.pixelStride * W
        val bmp = Bitmap.createBitmap(W + pad / pl.pixelStride, H, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(pl.buffer); img.close()
        return bmp
    }

    private fun scan() {
        overlay.visibility = View.INVISIBLE      // ẩn lớp phủ để không bị chụp vào
        h.postDelayed({
            val bmp = grab() ?: last
            if (bmp == null) { toast("Chưa chụp được màn hình, thử lại"); overlay.visibility = View.VISIBLE; return@postDelayed }
            last = bmp
            val cfg = Cfg.load(this)
            val pcs = Detector.pieces(bmp, W, H, cfg)
            val res = Solver.solve(Detector.board(bmp, W, H, cfg), pcs)
            overlay.cfg = cfg; overlay.moves = res
            overlay.msg = if (res == null) "Không có nước đi / không đọc được mảnh" else "Thứ tự: " + res.joinToString(" → ") { "mảnh ${it.pi + 1}" }
            overlay.visibility = View.VISIBLE; overlay.invalidate()
        }, 250)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        runCatching { wm.removeView(overlay); wm.removeView(btn) }
        vd?.release(); reader?.close(); projection?.stop()
        super.onDestroy()
    }
}

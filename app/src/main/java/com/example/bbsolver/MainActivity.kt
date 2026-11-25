package com.example.bbsolver

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val edits = LinkedHashMap<String, EditText>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sp = getSharedPreferences("cfg", 0)
        val ll = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 60, 40, 40) }
        ll.addView(TextView(this).apply {
            text = "Block Blast Solver\n1) Chỉnh vị trí bảng/khay cho khớp máy (tỉ lệ 0–1).\n2) Bấm Bắt đầu, cho phép quay màn hình.\n3) Vào game, bấm nút GIẢI nổi (nhấn giữ để tắt)."
        })
        for ((k, def) in Cfg.DEF) {
            ll.addView(TextView(this).apply { text = Cfg.LABEL[k]; setPadding(0, 24, 0, 0) })
            edits[k] = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(sp.getFloat(k, def).toString())
            }
            ll.addView(edits[k])
        }
        ll.addView(Button(this).apply { text = "Bắt đầu"; setOnClickListener { start() } })
        setContentView(ScrollView(this).apply { addView(ll, ViewGroup.LayoutParams(-1, -2)) })
    }

    private fun start() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Cấp quyền hiển thị trên ứng dụng khác rồi bấm Bắt đầu lại", Toast.LENGTH_LONG).show()
            return
        }
        val e = getSharedPreferences("cfg", 0).edit()
        edits.forEach { (k, v) -> e.putFloat(k, v.text.toString().toFloatOrNull() ?: Cfg.DEF[k]!!) }
        e.apply()
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), 1)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == 1 && res == RESULT_OK && data != null) {
            startForegroundService(Intent(this, CaptureService::class.java).putExtra("code", res).putExtra("data", data))
            moveTaskToBack(true)
        }
    }
}

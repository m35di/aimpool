package com.axion.aimassist

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager

    private lateinit var statusText: TextView

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val manageOverlayLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                requestProjection()
            } else {
                toast("اول باید اجازه Draw over other apps را بدهی.")
            }
        }

    private val projectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                startCaptureService(result.resultCode, result.data!!)
                statusText.text = "وضعیت: در حال اجرا"
            } else {
                toast("اجازه ضبط صفحه داده نشد.")
                statusText.text = "وضعیت: متوقف"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        statusText = findViewById(R.id.statusText)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val btnPing = findViewById<Button>(R.id.btnPing)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        btnStart.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                manageOverlayLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } else {
                requestProjection()
            }
        }

        btnStop.setOnClickListener {
            stopCaptureService()
            statusText.text = "وضعیت: متوقف"
        }

        btnPing.setOnClickListener {
            statusText.text = "وضعیت: تست سرور..."
            Thread {
                val ok = DetectClient().ping()
                runOnUiThread {
                    statusText.text = if (ok) {
                        "وضعیت: سرور آنلاین است"
                    } else {
                        "وضعیت: سرور در دسترس نیست"
                    }
                }
            }.start()
        }
    }

    private fun requestProjection() {
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun startCaptureService(resultCode: Int, data: Intent) {
        val intent = Intent(this, CaptureService::class.java).apply {
            action = CaptureService.ACTION_START
            putExtra(CaptureService.EXTRA_CODE, resultCode)
            putExtra(CaptureService.EXTRA_DATA, data)
        }

        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopCaptureService() {
        val intent = Intent(this, CaptureService::class.java).apply {
            action = CaptureService.ACTION_STOP
        }
        startService(intent)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

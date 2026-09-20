package com.axion.aimassist

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Base64
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class CaptureService : Service(), ImageReader.OnImageAvailableListener {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"

        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"

        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "aimassist_capture"
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var overlay: OverlayController? = null

    private val client = DetectClient()
    private val executor = Executors.newSingleThreadExecutor()

    private var lastProcessAt = 0L

    @Volatile
    private var inFlight = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            cleanupProjection()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )

                val resultCode = intent.getIntExtra(EXTRA_CODE, 0)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)

                if (resultCode != RESULT_OK || data == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                setupProjection(resultCode, data)
            }
        }

        return START_NOT_STICKY
    }

    private fun setupProjection(resultCode: Int, data: Intent) {
        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        projection = projectionManager.getMediaProjection(resultCode, data)
        projection?.registerCallback(projectionCallback, null)

        overlay = OverlayController(this)

        imageReader = ImageReader.newInstance(
            Config.CAPTURE_WIDTH,
            Config.CAPTURE_HEIGHT,
            PixelFormat.RGBA_8888,
            2
        )

        imageReader?.setOnImageAvailableListener(this, null)

        val metrics = resources.displayMetrics

        virtualDisplay = projection?.createVirtualDisplay(
            "AimAssistCapture",
            Config.CAPTURE_WIDTH,
            Config.CAPTURE_HEIGHT,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            null
        )
    }

    override fun onImageAvailable(reader: ImageReader) {
        val image: Image = reader.acquireLatestImage() ?: return

        val now = System.currentTimeMillis()

        if (inFlight || now - lastProcessAt < Config.DETECT_INTERVAL_MS) {
            image.close()
            return
        }

        lastProcessAt = now

        val bitmap = imageToBitmap(image)
        image.close()

        if (bitmap == null) return

        inFlight = true

        executor.execute {
            try {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, Config.JPEG_QUALITY, out)
                bitmap.recycle()

                val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                val result = client.detect(base64)

                overlay?.update(result)
            } catch (_: Exception) {
            } finally {
                inFlight = false
            }
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmapWidth = image.width + rowPadding / pixelStride

            val bitmap = Bitmap.createBitmap(
                bitmapWidth,
                image.height,
                Bitmap.Config.ARGB_8888
            )

            bitmap.copyPixelsFromBuffer(buffer)

            if (rowPadding == 0) {
                bitmap
            } else {
                Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildNotification(): Notification {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aim Assist Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, CaptureService::class.java).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aim Assist")
            .setContentText("در حال ضبط و تحلیل میز...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun cleanupProjection() {
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }

        try {
            imageReader?.close()
        } catch (_: Exception) {
        }

        overlay?.update(null)
        overlay?.destroy()

        virtualDisplay = null
        imageReader = null
        overlay = null
    }

    override fun onDestroy() {
        cleanupProjection()

        try {
            projection?.unregisterCallback(projectionCallback)
            projection?.stop()
        } catch (_: Exception) {
        }

        projection = null

        executor.shutdownNow()

        super.onDestroy()
    }
}

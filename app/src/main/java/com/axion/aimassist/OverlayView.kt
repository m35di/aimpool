package com.axion.aimassist

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.hypot

class OverlayView(context: Context) : View(context) {

    private var result: DetectResult? = null

    // برای نگاشت مختصات سرور → صفحه
    private var srcW = 1
    private var srcH = 1

    // رنگ‌ها
    private val ballFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val ballStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(200, 255, 255, 255)
    }

    private val cueBallStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.CYAN
    }

    private val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.argb(220, 0, 255, 120)
    }

    private val cueToGhostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        color = Color.RED
    }

    private val targetToPocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(230, 0, 255, 120)
    }

    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.YELLOW
    }

    private val ghostFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(90, 255, 255, 0)
    }

    private val extendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(120, 255, 255, 255)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        setShadowLayer(6f, 0f, 0f, Color.BLACK)
    }

    fun setResult(r: DetectResult?) {
        result = r
        if (r != null && r.width > 0 && r.height > 0) {
            srcW = r.width
            srcH = r.height
        }
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r = result ?: return
        if (!r.ok) return

        val scaleX = width.toFloat() / srcW.toFloat()
        val scaleY = height.toFloat() / srcH.toFloat()
        val avgScale = (scaleX + scaleY) / 2f

        // ۱) همه توپ‌ها با رنگ خودشون
        for (ball in r.balls) {
            val cx = ball.x * scaleX
            val cy = ball.y * scaleY
            val rad = ball.r * avgScale

            ballFillPaint.color = Color.argb(
                if (ball.type == "cue") 230 else 160,
                Color.red(ball.color),
                Color.green(ball.color),
                Color.blue(ball.color)
            )
            canvas.drawCircle(cx, cy, rad, ballFillPaint)

            val stroke = if (ball.type == "cue") cueBallStrokePaint else ballStrokePaint
            canvas.drawCircle(cx, cy, rad, stroke)
        }

        // ۲) پاکت‌ها
        for (p in r.pockets) {
            canvas.drawCircle(
                p.x * scaleX,
                p.y * scaleY,
                (p.r * avgScale).coerceAtLeast(12f),
                pocketPaint
            )
        }

        // ۳) aim
        val a = r.aim ?: return
        val cueX = a.cue.x * scaleX
        val cueY = a.cue.y * scaleY
        val ghostX = a.ghost.x * scaleX
        val ghostY = a.ghost.y * scaleY
        val targetX = a.target.x * scaleX
        val targetY = a.target.y * scaleY
        val pocketX = a.pocket.x * scaleX
        val pocketY = a.pocket.y * scaleY

        // خط ۱: cue → ghost
        canvas.drawLine(cueX, cueY, ghostX, ghostY, cueToGhostPaint)

        // خط ۲: امتداد پس از ghost (خیلی محو)
        val dx = ghostX - cueX
        val dy = ghostY - cueY
        val len = hypot(dx, dy).coerceAtLeast(1f)
        val ext = 3000f
        canvas.drawLine(
            ghostX, ghostY,
            ghostX + dx / len * ext,
            ghostY + dy / len * ext,
            extendPaint
        )

        // دایره ghost
        val ghostR = 18f * avgScale
        canvas.drawCircle(ghostX, ghostY, ghostR, ghostFillPaint)
        canvas.drawCircle(ghostX, ghostY, ghostR, ghostPaint)

        // خط ۳: target → pocket (مسیر توپ هدف)
        canvas.drawLine(targetX, targetY, pocketX, pocketY, targetToPocketPaint)

        // پاکت هدف رو پررنگ‌تر نشون بده
        val pocketHighlight = Paint(pocketPaint).apply {
            strokeWidth = 9f
            color = Color.YELLOW
        }
        canvas.drawCircle(pocketX, pocketY, 22f * avgScale, pocketHighlight)

        // متن اطلاعات
        val info = "conf %.0f%%  type %s".format(a.confidence * 100, a.targetType)
        canvas.drawText(info, 60f, 90f, textPaint)
    }
}

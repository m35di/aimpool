package com.axion.aimassist

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class OverlayView(context: Context) : View(context) {

    private var result: DetectResult? = null

    private var scaleX = 1f
    private var scaleY = 1f

    private val aimLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 7f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 255, 255, 255)
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val cuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 0, 200, 255)
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 215, 0)
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 0, 255, 120)
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scaleX = w.toFloat() / Config.CAPTURE_WIDTH.toFloat()
        scaleY = h.toFloat() / Config.CAPTURE_HEIGHT.toFloat()
    }

    fun setResult(r: DetectResult?) {
        result = r
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val r = result ?: return
        if (!r.ok) return

        val avgScale = (scaleX + scaleY) / 2f

        for (ball in r.balls) {
            canvas.drawCircle(
                ball.x * scaleX,
                ball.y * scaleY,
                ball.r * avgScale,
                ballPaint
            )
        }

        r.cue?.let { cue ->
            canvas.drawCircle(
                cue.x * scaleX,
                cue.y * scaleY,
                r.cueR * avgScale,
                cuePaint
            )
        }

        r.ghost?.let { ghost ->
            canvas.drawCircle(
                ghost.x * scaleX,
                ghost.y * scaleY,
                r.ghostR * avgScale,
                ghostPaint
            )
        }

        r.pocket?.let { pocket ->
            canvas.drawCircle(
                pocket.x * scaleX,
                pocket.y * scaleY,
                16f * avgScale,
                pocketPaint
            )
        }

        val from = r.cue
        val via = r.ghost
        val to = r.aimTo ?: r.pocket

        if (from != null && via != null) {
            canvas.drawLine(
                from.x * scaleX,
                from.y * scaleY,
                via.x * scaleX,
                via.y * scaleY,
                aimLinePaint
            )

            if (to != null) {
                canvas.drawLine(
                    via.x * scaleX,
                    via.y * scaleY,
                    to.x * scaleX,
                    to.y * scaleY,
                    aimLinePaint
                )
            } else {
                val dx = via.x - from.x
                val dy = via.y - from.y
                val len = kotlin.math.sqrt(dx * dx + dy * dy)

                if (len > 1f) {
                    val extend = 5000f
                    val nx = dx / len
                    val ny = dy / len

                    canvas.drawLine(
                        via.x * scaleX,
                        via.y * scaleY,
                        (via.x + nx * extend) * scaleX,
                        (via.y + ny * extend) * scaleY,
                        aimLinePaint
                    )
                }
            }
        }
    }
}

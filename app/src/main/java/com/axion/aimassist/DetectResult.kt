package com.axion.aimassist

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

data class Pt(val x: Float, val y: Float)

data class Ball(
    val x: Float,
    val y: Float,
    val r: Float,
    val type: String,      // "cue" | "color" | "black"
    val color: Int          // ARGB
)

data class Aim(
    val cue: Pt,
    val ghost: Pt,
    val target: Pt,
    val pocket: Pt,
    val targetType: String,
    val confidence: Float
)

data class DetectResult(
    val ok: Boolean,
    val width: Int,
    val height: Int,
    val balls: List<Ball>,
    val pockets: List<Ball>,
    val aim: Aim?
) {
    companion object {
        fun parse(body: String): DetectResult? {
            return try {
                val o = JSONObject(body)

                if (!o.optBoolean("ok", false)) return null

                val width = o.optInt("width", 0)
                val height = o.optInt("height", 0)
                if (width <= 0 || height <= 0) return null

                // balls
                val balls = ArrayList<Ball>()
                o.optJSONArray("balls")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        parseBall(arr.optJSONObject(i))?.let { balls.add(it) }
                    }
                }

                // pockets (structure همان balls ولی type="pocket")
                val pockets = ArrayList<Ball>()
                o.optJSONArray("pockets")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        parseBall(arr.optJSONObject(i))?.let { pockets.add(it) }
                    }
                }

                // aim
                val aim = parseAim(o.optJSONObject("aim"))

                DetectResult(
                    ok = true,
                    width = width,
                    height = height,
                    balls = balls,
                    pockets = pockets,
                    aim = aim
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun parseBall(b: JSONObject?): Ball? {
            if (b == null) return null
            val x = b.optDouble("x", Double.NaN).toFloat()
            val y = b.optDouble("y", Double.NaN).toFloat()
            val r = b.optDouble("r", 12.0).toFloat()
            if (x.isNaN() || y.isNaN() || r <= 0f) return null

            val type = b.optString("type", "color")

            // سرور [B,G,R] می‌فرسته (OpenCV default)
            var argb = Color.WHITE
            b.optJSONArray("color")?.let { c ->
                val bVal = c.optInt(0, 200).coerceIn(0, 255)
                val gVal = c.optInt(1, 200).coerceIn(0, 255)
                val rVal = c.optInt(2, 200).coerceIn(0, 255)
                argb = Color.rgb(rVal, gVal, bVal)
            }

            return Ball(x, y, r, type, argb)
        }

        private fun parseAim(a: JSONObject?): Aim? {
            if (a == null) return null

            val cue    = readPt(a.optJSONArray("cue"))    ?: return null
            val ghost  = readPt(a.optJSONArray("ghost"))  ?: return null
            val target = readPt(a.optJSONArray("target")) ?: return null
            val pocket = readPt(a.optJSONArray("pocket")) ?: return null

            return Aim(
                cue = cue,
                ghost = ghost,
                target = target,
                pocket = pocket,
                targetType = a.optString("target_type", ""),
                confidence = a.optDouble("confidence", 0.0).toFloat()
            )
        }

        private fun readPt(arr: JSONArray?): Pt? {
            if (arr == null || arr.length() < 2) return null
            return Pt(
                arr.optDouble(0, Double.NaN).toFloat(),
                arr.optDouble(1, Double.NaN).toFloat()
            )
        }
    }
}

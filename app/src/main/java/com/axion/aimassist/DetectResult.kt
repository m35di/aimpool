package com.axion.aimassist

import org.json.JSONObject

data class Pt(val x: Float, val y: Float)

data class Ball(
    val x: Float,
    val y: Float,
    val r: Float,
    val color: String
)

data class DetectResult(
    val ok: Boolean,
    val balls: List<Ball>,
    val cue: Pt?,
    val cueR: Float,
    val ghost: Pt?,
    val ghostR: Float,
    val aimTo: Pt?,
    val pocket: Pt?
) {
    companion object {
        fun parse(body: String): DetectResult? {
            return try {
                val o = JSONObject(body)

                val balls = mutableListOf<Ball>()
                o.optJSONArray("balls")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val b = arr.optJSONObject(i) ?: continue
                        balls.add(
                            Ball(
                                x = b.optDouble("x", 0.0).toFloat(),
                                y = b.optDouble("y", 0.0).toFloat(),
                                r = b.optDouble("r", 12.0).toFloat(),
                                color = b.optString("color", "")
                            )
                        )
                    }
                }

                val cueObj = o.optJSONObject("cue")
                val cue = cueObj?.let {
                    Pt(
                        it.optDouble("x", 0.0).toFloat(),
                        it.optDouble("y", 0.0).toFloat()
                    )
                }
                val cueR = cueObj?.optDouble("r", 12.0)?.toFloat() ?: 12f

                val ghostObj = o.optJSONObject("ghost")
                val ghost = ghostObj?.let {
                    Pt(
                        it.optDouble("x", 0.0).toFloat(),
                        it.optDouble("y", 0.0).toFloat()
                    )
                }
                val ghostR = ghostObj?.optDouble("r", 12.0)?.toFloat() ?: 12f

                val aim = o.optJSONObject("aim")

                val aimTo = aim?.optJSONObject("to")?.let {
                    Pt(
                        it.optDouble("x", 0.0).toFloat(),
                        it.optDouble("y", 0.0).toFloat()
                    )
                }

                val pocket = aim?.optJSONObject("pocket")?.let {
                    Pt(
                        it.optDouble("x", 0.0).toFloat(),
                        it.optDouble("y", 0.0).toFloat()
                    )
                }

                DetectResult(
                    ok = o.optBoolean("ok", true),
                    balls = balls,
                    cue = cue,
                    cueR = cueR,
                    ghost = ghost,
                    ghostR = ghostR,
                    aimTo = aimTo,
                    pocket = pocket
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

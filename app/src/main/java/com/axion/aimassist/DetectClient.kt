package com.axion.aimassist

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DetectClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    fun detect(base64Jpeg: String): DetectResult? {
        return try {
            val payload = JSONObject()
                .put("image", base64Jpeg)
                .put("width", Config.CAPTURE_WIDTH)
                .put("height", Config.CAPTURE_HEIGHT)
                .toString()

            val body = payload.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(Config.DETECT_URL)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val text = response.body?.string() ?: return null
                DetectResult.parse(text)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun ping(): Boolean {
        return try {
            val request = Request.Builder()
                .url(Config.PING_URL)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}

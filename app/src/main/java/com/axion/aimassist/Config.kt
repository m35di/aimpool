package com.axion.aimassist

object Config {
    const val BASE_URL = "http://31.56.28.121/5e34bbb52bf243039bedaaae998ac947"
    const val PING_URL = "$BASE_URL/ping"
    const val DETECT_URL = "$BASE_URL/detect"

    const val CAPTURE_WIDTH = 720
    const val CAPTURE_HEIGHT = 1560

    const val JPEG_QUALITY = 60
    const val DETECT_INTERVAL_MS = 350L
}

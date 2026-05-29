package com.example.pulse

object SmsReceiverDedup {

    private const val DEDUP_WINDOW_MS = 3_000L

    fun isDuplicate(
        newBody: String,
        newTime: Long,
        lastBody: String,
        lastTime: Long,
    ): Boolean {
        return newBody == lastBody && (newTime - lastTime) < DEDUP_WINDOW_MS
    }
}

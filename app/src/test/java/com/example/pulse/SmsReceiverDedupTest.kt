package com.example.pulse

import org.junit.Assert.*
import org.junit.Test

class SmsReceiverDedupTest {

    @Test
    fun `same body within 3 seconds is duplicate`() {
        val body = "ALERT: server down"
        val now = System.currentTimeMillis()
        assertTrue(SmsReceiverDedup.isDuplicate(body, now, body, now - 1000))
    }

    @Test
    fun `same body after 3 seconds is not duplicate`() {
        val body = "ALERT: server down"
        val now = System.currentTimeMillis()
        assertFalse(SmsReceiverDedup.isDuplicate(body, now, body, now - 4000))
    }

    @Test
    fun `different body is never duplicate`() {
        val now = System.currentTimeMillis()
        assertFalse(SmsReceiverDedup.isDuplicate("first", now, "second", now - 500))
    }

    @Test
    fun `empty body is not duplicate of non-empty`() {
        val now = System.currentTimeMillis()
        assertFalse(SmsReceiverDedup.isDuplicate("", now, "something", now - 500))
    }
}

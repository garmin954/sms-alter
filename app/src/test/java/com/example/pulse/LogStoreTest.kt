package com.example.pulse

import org.junit.Assert.*
import org.junit.Test

class LogStoreTest {

    @Test
    fun `add stores entry at head`() {
        LogStore.clear()
        LogStore.i("second")
        LogStore.i("first")

        val entries = LogStore.entries
        assertEquals(2, entries.size)
        assertTrue(entries[0].contains("first"))
        assertTrue(entries[1].contains("second"))
    }

    @Test
    fun `clear removes all entries`() {
        LogStore.clear()
        LogStore.i("something")
        LogStore.clear()
        assertTrue(LogStore.entries.isEmpty())
    }

    @Test
    fun `d i w e produce correct tags`() {
        LogStore.clear()
        LogStore.d("debug")
        LogStore.i("info")
        LogStore.w("warn")
        LogStore.e("error")

        val entries = LogStore.entries
        assertTrue(entries[3].contains("[D]"))
        assertTrue(entries[2].contains("[I]"))
        assertTrue(entries[1].contains("[W]"))
        assertTrue(entries[0].contains("[E]"))
    }

    @Test
    fun `max entries is capped at 500`() {
        LogStore.clear()
        repeat(600) { i -> LogStore.i("entry $i") }
        assertEquals(500, LogStore.entries.size)
        assertTrue(LogStore.entries.last().contains("entry 100"))
    }

    @Test
    fun `concurrent access does not crash`() {
        LogStore.clear()
        val threads = List(10) { t ->
            Thread { repeat(100) { m -> LogStore.i("t$t m$m") } }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertFalse(LogStore.entries.isEmpty())
    }
}

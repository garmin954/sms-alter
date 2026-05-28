package com.example.smsalert

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class KeywordStoreTest {

    @Test
    fun `keywords serialized as JSON array round-trip correctly`() {
        val list = listOf("ALERT", "紧急", "test123")
        val json = JSONArray(list).toString()
        val parsed = JSONArray(json)
        val result = mutableListOf<String>()
        for (i in 0 until parsed.length()) {
            result.add(parsed.getString(i))
        }
        assertEquals(list, result)
    }

    @Test
    fun `JSON array handles single keyword`() {
        val json = JSONArray(listOf("onlyone")).toString()
        val parsed = JSONArray(json)
        assertEquals("onlyone", parsed.getString(0))
        assertEquals(1, parsed.length())
    }

    @Test
    fun `JSON array handles empty list`() {
        val json = JSONArray(emptyList<String>()).toString()
        val parsed = JSONArray(json)
        assertEquals(0, parsed.length())
    }

    @Test
    fun `JSON array handles special characters`() {
        val special = listOf("hello##world", "foo##bar", "test##value")
        val json = JSONArray(special).toString()
        val parsed = JSONArray(json)
        val result = mutableListOf<String>()
        for (i in 0 until parsed.length()) {
            result.add(parsed.getString(i))
        }
        assertEquals(special, result)
    }
}

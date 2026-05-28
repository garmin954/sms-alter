package com.example.smsalert

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object LogStore {

    private const val MAX_ENTRIES = 500

    private val _entries = CopyOnWriteArrayList<String>()
    val entries: List<String> get() = _entries.toList()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    @Volatile
    var onNewEntry: (() -> Unit)? = null

    @Synchronized
    fun add(tag: String, msg: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] [$tag] $msg"
        _entries.add(0, entry)
        if (_entries.size > MAX_ENTRIES) {
            _entries.removeAt(_entries.size - 1)
        }
        onNewEntry?.invoke()
    }

    fun clear() {
        _entries.clear()
    }

    fun d(msg: String) = add("D", msg)
    fun i(msg: String) = add("I", msg)
    fun w(msg: String) = add("W", msg)
    fun e(msg: String) = add("E", msg)
}

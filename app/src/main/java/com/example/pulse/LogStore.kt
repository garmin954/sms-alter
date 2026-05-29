package com.example.pulse

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object LogStore {

    private const val MAX_ENTRIES = 500

    private val _entries = CopyOnWriteArrayList<String>()
    val entries: List<String> get() = _entries.toList()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    @Synchronized
    fun add(tag: String, msg: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] [$tag] $msg"
        _entries.add(0, entry)
        if (_entries.size > MAX_ENTRIES) {
            _entries.removeAt(_entries.size - 1)
        }
        _events.tryEmit(Unit)
    }

    fun clear() {
        _entries.clear()
    }

    fun d(msg: String) = add("D", msg)
    fun i(msg: String) = add("I", msg)
    fun w(msg: String) = add("W", msg)
    fun e(msg: String) = add("E", msg)
}

package com.example.pulse

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object LogStore {

    private const val MAX_ENTRIES = 500
    private const val MAX_FILE_SIZE_BYTES = 200_000L
    private const val LOG_FILE_NAME = "pulse_events.log"

    private val _entries = CopyOnWriteArrayList<String>()
    val entries: List<String> get() = _entries.toList()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    @Volatile
    private var logFile: File? = null
    private val fileLock = Any()

    /**
     * 必须在 Application.onCreate() 中尽早调用，否则文件日志不生效。
     * 调用后会从文件中恢复上次进程的日志。
     */
    fun init(context: Context) {
        synchronized(fileLock) {
            if (logFile != null) return
            logFile = File(context.filesDir, LOG_FILE_NAME)
            // 从文件恢复上一条进程的日志
            loadFromFile()
        }
    }

    private fun loadFromFile() {
        val file = logFile ?: return
        if (!file.exists()) return
        try {
            val lines = file.readLines()
            // 取最后 MAX_ENTRIES 条，倒序插入内存（最新的在前）
            val recent = lines.takeLast(MAX_ENTRIES)
            for (line in recent.reversed()) {
                _entries.add(line)
            }
            if (recent.isNotEmpty()) {
                _events.tryEmit(Unit)
            }
        } catch (e: Exception) {
            Log.e("LogStore", "读取持久化日志失败", e)
        }
    }

    @Synchronized
    fun add(tag: String, msg: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] [$tag] $msg"
        _entries.add(0, entry)
        if (_entries.size > MAX_ENTRIES) {
            _entries.removeAt(_entries.size - 1)
        }
        _events.tryEmit(Unit)
        // 持久化到文件
        writeToFile(entry)
    }

    private fun writeToFile(entry: String) {
        val file = logFile ?: return
        synchronized(fileLock) {
            try {
                file.parentFile?.mkdirs()
                file.appendText(entry + "\n")
            } catch (e: Exception) {
                Log.e("LogStore", "写入持久化日志失败", e)
            }
            if (file.length() > MAX_FILE_SIZE_BYTES) {
                truncateFile(file)
            }
            Unit
        }
    }

    private fun truncateFile(file: File) {
        try {
            val allLines = file.readLines()
            val keep = allLines.takeLast(MAX_ENTRIES)
            file.writeText(keep.joinToString("\n") + "\n")
        } catch (e: Exception) {
            Log.e("LogStore", "日志文件截断失败", e)
        }
    }

    fun clear() {
        _entries.clear()
        synchronized(fileLock) {
            try {
                logFile?.delete()
            } catch (_: Exception) {}
        }
    }

    fun d(msg: String) = add("D", msg)
    fun i(msg: String) = add("I", msg)
    fun w(msg: String) = add("W", msg)
    fun e(msg: String) = add("E", msg)
}

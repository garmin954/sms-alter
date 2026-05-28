package com.example.smsalert.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.smsalert.data.dao.AlertDao
import com.example.smsalert.data.entity.AlertRecord

@Database(
    entities = [AlertRecord::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
}

package com.example.pulse.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pulse.data.dao.AlertDao
import com.example.pulse.data.entity.AlertRecord

@Database(
    entities = [AlertRecord::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
}

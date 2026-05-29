package com.example.pulse.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pulse.data.entity.AlertRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: AlertRecord)

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAll(): Flow<List<AlertRecord>>

    @Query("SELECT * FROM alerts WHERE message LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<AlertRecord>>

    @Query("SELECT COUNT(*) FROM alerts WHERE timestamp >= :since")
    suspend fun countSince(since: Long): Int

    @Query("DELETE FROM alerts")
    suspend fun clearAll()

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

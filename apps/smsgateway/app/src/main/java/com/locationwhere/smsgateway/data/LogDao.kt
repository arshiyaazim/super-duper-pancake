package com.locationwhere.smsgateway.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): LiveData<List<LogEntry>>

    @Insert
    suspend fun insert(log: LogEntry)

    @Query("DELETE FROM logs WHERE id NOT IN (SELECT id FROM logs ORDER BY timestamp DESC LIMIT 100)")
    suspend fun clearOldLogs()

    @Query("DELETE FROM logs")
    suspend fun deleteAll()
}

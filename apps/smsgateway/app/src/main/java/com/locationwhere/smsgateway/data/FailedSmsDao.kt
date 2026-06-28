package com.locationwhere.smsgateway.data

import androidx.room.*

@Dao
interface FailedSmsDao {
    @Query("SELECT * FROM failed_sms WHERE nextRetryAt <= :now AND retryCount < :maxRetries ORDER BY timestamp ASC LIMIT 10")
    suspend fun getPendingRetries(now: Long = System.currentTimeMillis(), maxRetries: Int = 5): List<FailedSmsEntry>

    @Insert
    suspend fun insert(entry: FailedSmsEntry)

    @Delete
    suspend fun delete(entry: FailedSmsEntry)

    @Query("UPDATE failed_sms SET retryCount = retryCount + 1, nextRetryAt = :nextRetry WHERE id = :id")
    suspend fun updateRetry(id: Long, nextRetry: Long)

    @Query("DELETE FROM failed_sms WHERE retryCount >= :maxRetries")
    suspend fun deleteExhaustedRetries(maxRetries: Int = 5)

    @Query("SELECT COUNT(*) FROM failed_sms")
    suspend fun getCount(): Int
}

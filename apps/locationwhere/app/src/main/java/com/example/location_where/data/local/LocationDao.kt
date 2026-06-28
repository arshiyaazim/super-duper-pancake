package com.example.location_where.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Query("SELECT * FROM location_logs WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedLocations(): List<LocationEntity>

    @Query("UPDATE location_logs SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("DELETE FROM location_logs WHERE isSynced = 1 AND timestamp < :beforeTimestamp")
    suspend fun deleteOldSyncedLocations(beforeTimestamp: Long)
}

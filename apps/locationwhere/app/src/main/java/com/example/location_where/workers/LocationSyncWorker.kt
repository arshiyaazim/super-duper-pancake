package com.example.location_where.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.location_where.data.repository.LocationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LocationSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: LocationRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("LocationSyncWorker", "Starting pending location upload")
            repository.uploadPendingLocations()
            Log.d("LocationSyncWorker", "Pending location upload finished successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("LocationSyncWorker", "Pending location upload failed", e)
            Result.retry()
        }
    }
}

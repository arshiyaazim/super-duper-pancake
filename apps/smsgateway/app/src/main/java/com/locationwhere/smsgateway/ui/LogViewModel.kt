package com.locationwhere.smsgateway.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.locationwhere.smsgateway.data.AppDatabase
import com.locationwhere.smsgateway.data.LogDao
import com.locationwhere.smsgateway.data.LogEntry
import kotlinx.coroutines.launch

class LogViewModel(application: Application) : AndroidViewModel(application) {
    private val logDao: LogDao = AppDatabase.getDatabase(application).logDao()
    val allLogs: LiveData<List<LogEntry>> = logDao.getAllLogs()

    fun deleteAllLogs() {
        viewModelScope.launch {
            logDao.deleteAll()
        }
    }
}

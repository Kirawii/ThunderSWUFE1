package com.kirawii.thunderswufe.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import com.kirawii.thunderswufe.work.ElectricityCheckWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val electricityDao = (application as ThunderApplication).database.electricityDao()
    private val workManager = WorkManager.getInstance(application.applicationContext)

    private val _records = MutableStateFlow<List<ElectricityRecord>>(emptyList())
    val records: StateFlow<List<ElectricityRecord>> = _records.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            // Observe database changes for all records, ordered by timestamp descending
            // HomeScreen will reverse this list for the chart if needed.
            electricityDao.getAllRecords().collect { newRecords ->
                _records.value = newRecords
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch // Prevent multiple refreshes if already in progress

            _isRefreshing.value = true
            try {
                // Enqueue a one-time work request to fetch new data
                val workRequest = OneTimeWorkRequestBuilder<ElectricityCheckWorker>().build()
                workManager.enqueue(workRequest)
                
                // Optional: You might want to observe the WorkInfo to know when the worker actually finishes.
                // For now, we'll assume the data will flow in once the worker updates the DB.
                // The _isRefreshing state here is mostly for UI feedback during the enqueue process.
                // We can set it to false after a short delay or rely on data changes to imply completion.

                // For immediate UI feedback that the refresh action was triggered:
                // Consider setting _isRefreshing to false after a short delay,
                // or when the new data actually arrives and _records is updated.
                // For this example, let's set it to false after a brief moment to indicate the action is "done".
                // A more robust solution involves listening to WorkManager's WorkInfo.
                delay(1500) // Simulate that the refresh action has been initiated.
            } finally {
                _isRefreshing.value = false
            }
        }
    }
} 
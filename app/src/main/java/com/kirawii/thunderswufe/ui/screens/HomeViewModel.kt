package com.kirawii.thunderswufe.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import com.kirawii.thunderswufe.work.ElectricityCheckWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.kirawii.thunderswufe.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    application: Application,
    private val settingsViewModel: SettingsViewModel
) : AndroidViewModel(application) {

    private val electricityDao = (application as ThunderApplication).database.electricityDao()
    private val workManager = WorkManager.getInstance(application.applicationContext)
    private val predictor = (application as ThunderApplication).electricityPredictor

    private val _records = MutableStateFlow<List<ElectricityRecord>>(emptyList())
    val records: StateFlow<List<ElectricityRecord>> = _records.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshMessage = MutableStateFlow<String?>(null)
    val refreshMessage: StateFlow<String?> = _refreshMessage.asStateFlow()

    private val _predictedDays = MutableStateFlow<Int?>(null)
    val predictedDays: StateFlow<Int?> = _predictedDays.asStateFlow()

    private val _predictionError = MutableStateFlow<String?>(null)
    val predictionError: StateFlow<String?> = _predictionError.asStateFlow()

    private val _isPredicting = MutableStateFlow(false)
    val isPredicting: StateFlow<Boolean> = _isPredicting.asStateFlow()

    init {
        viewModelScope.launch {
            settingsViewModel.roomNoFlow
                .flatMapLatest { roomNo -> electricityDao.getAllRecordsByRoom(roomNo) }
                .collect { newRecords ->
                    _records.value = newRecords
                    runPredictionWithRecords(newRecords)
                }
        }
    }

    private fun runPredictionWithRecords(records: List<ElectricityRecord>) {
        viewModelScope.launch {
            if (records.isEmpty()) {
                _predictedDays.value = null
                _predictionError.value = "无历史数据"
                return@launch
            }
            _isPredicting.value = true
            _predictionError.value = null
            try {
                val result = withContext(Dispatchers.Default) {
                    predictor.predictFutureUsage(records)
                }
                _predictedDays.value = result.daysUntilEmpty
                _predictionError.value = result.error
            } catch (e: Exception) {
                _predictedDays.value = null
                _predictionError.value = "预测失败: ${e.localizedMessage}"
            } finally {
                _isPredicting.value = false
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch

            _isRefreshing.value = true
            _refreshMessage.value = null
            try {
                val workRequest = OneTimeWorkRequestBuilder<ElectricityCheckWorker>().build()
                workManager.enqueueUniqueWork(
                    "manual_electricity_refresh",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                val workInfo = workManager.getWorkInfoByIdFlow(workRequest.id)
                    .filterNotNull()
                    .first { it.state.isFinished }
                _refreshMessage.value = when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> workInfo.outputData.getString(ElectricityCheckWorker.OUTPUT_MESSAGE)
                        ?: "更新成功"
                    WorkInfo.State.FAILED -> workInfo.outputData.getString(ElectricityCheckWorker.OUTPUT_MESSAGE)
                        ?: "更新失败"
                    WorkInfo.State.CANCELLED -> "更新已取消"
                    else -> null
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

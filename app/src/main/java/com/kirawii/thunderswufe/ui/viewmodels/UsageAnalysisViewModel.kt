package com.kirawii.thunderswufe.ui.viewmodels // ViewModel 通常放在 viewmodels 包

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import com.kirawii.thunderswufe.ml.ElectricityPredictor
import com.kirawii.thunderswufe.ml.ModelType
import com.kirawii.thunderswufe.ml.PredictionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UsageAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val electricityDao = (application as ThunderApplication).database.electricityDao()
    private val predictor: ElectricityPredictor = (application as ThunderApplication).electricityPredictor

    private val _historicalRecords = MutableStateFlow<List<ElectricityRecord>>(emptyList())
    val historicalRecords: StateFlow<List<ElectricityRecord>> = _historicalRecords.asStateFlow()

    private val _predictionResult = MutableStateFlow<PredictionResult?>(null)
    val predictionResult: StateFlow<PredictionResult?> = _predictionResult.asStateFlow()

    private val _isLoadingPrediction = MutableStateFlow(false)
    val isLoadingPrediction: StateFlow<Boolean> = _isLoadingPrediction.asStateFlow()

    init {
        viewModelScope.launch {
            electricityDao.getAllRecords().collect { records ->
                _historicalRecords.value = records
            }
        }
    }

    fun runPrediction(modelType: ModelType) {
        viewModelScope.launch {
            if (_historicalRecords.value.isEmpty()) {
                Log.w("UsageAnalysisVM", "No historical records available to run prediction.")
                _predictionResult.value = PredictionResult(null, emptyList(), 0f, "没有足够的历史数据")
                return@launch
            }
            _isLoadingPrediction.value = true
            _predictionResult.value = null
            try {
                val result = predictor.predictFutureUsage(
                    records = _historicalRecords.value,
                    modelTypeToUse = modelType
                )
                _predictionResult.value = result
            } catch (e: Exception) {
                Log.e("UsageAnalysisVM", "Error running prediction with $modelType: ${e.message}", e)
                _predictionResult.value = PredictionResult(null, emptyList(), 0f, "预测时发生错误: ${e.localizedMessage}")
            } finally {
                _isLoadingPrediction.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("UsageAnalysisVM", "ViewModel cleared.")
    }
} 
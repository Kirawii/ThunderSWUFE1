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
    // 获取 Predictor 实例的方式，确保只创建一个或从 Application 获取
    private val predictor: ElectricityPredictor = (application as ThunderApplication).electricityPredictor

    private val _historicalRecords = MutableStateFlow<List<ElectricityRecord>>(emptyList())
    val historicalRecords: StateFlow<List<ElectricityRecord>> = _historicalRecords.asStateFlow()

    private val _predictionResult = MutableStateFlow<PredictionResult?>(null)
    val predictionResult: StateFlow<PredictionResult?> = _predictionResult.asStateFlow()

    private val _isLoadingPrediction = MutableStateFlow(false)
    val isLoadingPrediction: StateFlow<Boolean> = _isLoadingPrediction.asStateFlow()

    init {
        viewModelScope.launch {
            // 加载所有历史记录用于分析和预测
            electricityDao.getAllRecords().collect { records ->
                _historicalRecords.value = records
                // Optionally, run a default prediction when records are loaded
                // if (records.isNotEmpty()) {
                //     runPrediction(ModelType.LSTM) // Example: default to LSTM
                // }
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
            _predictionResult.value = null // Clear previous result
            try {
                // 在后台线程执行预测，因为它可能涉及文件IO和计算
                // kotlinx.coroutines.withContext(Dispatchers.IO) { // 如果预测耗时
                // }
                val result = predictor.predictFutureUsage(
                    records = _historicalRecords.value, // 使用收集到的所有记录
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
        // predictor.close() // 在 Application 级别或需要更长生命周期时，不应在此关闭
        // 如果 Predictor 是 ViewModel 独占的，可以在此 close。
        // 但由于您在 ThunderApplication 中初始化了 predictor，它可能是共享的。
        // 如果 predictor 在 Application 中创建并作为单例使用，则其 close() 应由 Application 管理，
        // 或者 Predictor 自身设计为可多次调用而无需每次关闭再打开。
        // 目前 ElectricityPredictor 的 close() 会将 interpreter 置为 null，
        // 所以如果它是共享的，不能在这里简单关闭。
        Log.d("UsageAnalysisVM", "ViewModel cleared.")
    }
} 
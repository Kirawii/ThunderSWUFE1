package com.kirawii.thunderswufe.ml

import android.content.Context
import android.util.Log
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max

enum class ModelType {
    LSTM,
    LINEAR_REGRESSION_KERAS,
    SIMPLE_LINEAR
}

class ElectricityPredictor(private val context: Context) {
    private var lstmInterpreter: Interpreter? = null
    private var lrKerasInterpreter: Interpreter? = null

    // 模型文件名常量
    companion object {
        private const val LSTM_MODEL_FILE = "lstm_model.tflite"
        private const val LR_KERAS_MODEL_FILE = "linear_regression_keras.tflite"
        private const val SCALER_PARAMS_FILE = "scaler_params.json"
        private const val DEFAULT_INPUT_LENGTH = 7
        private const val MODEL_OUTPUT_STEPS = 1
        private const val PREDICTION_HORIZON_DAYS = 7
    }

    private var scalerScale: FloatArray? = null
    private var scalerMin: FloatArray? = null

    private val floatComparisonThreshold = 1e-6f

    init {
        loadAllModelsAndParams()
    }

    private fun loadAllModelsAndParams() {
        lstmInterpreter = loadTFLiteModel(LSTM_MODEL_FILE)
        if (lstmInterpreter != null) {
            Log.i("ElectricityPredictor", "LSTM model '$LSTM_MODEL_FILE' loaded successfully.")
        } else {
            Log.e("ElectricityPredictor", "Failed to load LSTM model '$LSTM_MODEL_FILE'.")
        }

        lrKerasInterpreter = loadTFLiteModel(LR_KERAS_MODEL_FILE)
        if (lrKerasInterpreter != null) {
            Log.i("ElectricityPredictor", "Keras Linear Regression model '$LR_KERAS_MODEL_FILE' loaded successfully.")
        } else {
            Log.e("ElectricityPredictor", "Failed to load Keras Linear Regression model '$LR_KERAS_MODEL_FILE'.")
        }

        loadScalerParams(SCALER_PARAMS_FILE)
        if (scalerScale == null || scalerMin == null) {
            Log.e("ElectricityPredictor", "Failed to load scaler parameters from '$SCALER_PARAMS_FILE'. Predictions might be inaccurate.")
        }
    }

    @Throws(IOException::class)
    private fun loadTFLiteModel(modelFileName: String): Interpreter? {
        return try {
            context.assets.openFd(modelFileName).use { fileDescriptor ->
                FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                    val fileChannel = inputStream.channel
                    val startOffset = fileDescriptor.startOffset
                    val declaredLength = fileDescriptor.declaredLength
                    val mappedByteBuffer =
                        fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                    Interpreter(mappedByteBuffer)
                }
            }
        } catch (e: Exception) {
            Log.e("ElectricityPredictor", "Error loading TFLite model '$modelFileName': ${e.message}", e)
            null
        }
    }

    private fun loadScalerParams(fileName: String) {
        try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val scaleJsonArray = jsonObject.getJSONArray("scale_")
            val minJsonArray = jsonObject.getJSONArray("min_")

            scalerScale = FloatArray(scaleJsonArray.length()) { i -> scaleJsonArray.getDouble(i).toFloat() }
            scalerMin = FloatArray(minJsonArray.length()) { i -> minJsonArray.getDouble(i).toFloat() }
            Log.i("ElectricityPredictor", "Scaler params loaded: scale=${scalerScale?.contentToString()}, min=${scalerMin?.contentToString()}")

        } catch (e: Exception) {
            Log.e("ElectricityPredictor", "Error loading scaler params '$fileName': ${e.message}", e)
            scalerScale = null
            scalerMin = null
        }
    }
    private fun scaleUsage(rawValue: Float): Float {
        if (scalerScale == null || scalerMin == null || scalerScale!!.isEmpty()) {
            Log.w("ElectricityPredictor", "Scaler not initialized, returning raw value for scaling.")
            return rawValue
        }
        return rawValue * scalerScale!![0] + scalerMin!![0]
    }

    private fun inverseScaleUsage(scaledValue: Float): Float {
        if (scalerScale == null || scalerMin == null || scalerScale!!.isEmpty() || scalerScale!![0] == 0f) {
            Log.w("ElectricityPredictor", "Scaler not initialized or scale is zero, returning scaled value for inverse scaling.")
            return scaledValue
        }
        return (scaledValue - scalerMin!![0]) / scalerScale!![0]
    }


    fun predictFutureUsage(
        records: List<ElectricityRecord>,
        modelTypeToUse: ModelType = ModelType.LSTM
    ): PredictionResult {
        if (records.isEmpty()) {
            return PredictionResult(null, emptyList(), 0.0f, "没有历史数据")
        }

        val sortedRecords = records.sortedBy { it.timestamp }
        val currentBalance = sortedRecords.last().balance

        if (sortedRecords.size < DEFAULT_INPUT_LENGTH) {
            Log.w("ElectricityPredictor", "历史数据不足 (需要 $DEFAULT_INPUT_LENGTH, 现有 ${sortedRecords.size})，无法进行预测。")
            return PredictionResult(null, emptyList(), 0.0f, "历史数据不足以进行任何预测")
        }

        val initialRawHistory = sortedRecords
            .takeLast(DEFAULT_INPUT_LENGTH)
            .map { it.change.toFloat() }

        if (initialRawHistory.size < DEFAULT_INPUT_LENGTH) {
            Log.e("ElectricityPredictor", "Logic error: Not enough data for initial history after filtering.")
            return PredictionResult(null, emptyList(), 0.0f, "内部错误：准备历史数据失败")
        }
        val initialScaledHistory = initialRawHistory.map { scaleUsage(it) }.toMutableList()

        val activeInterpreter: Interpreter?
        val modelNameForLog: String
        when (modelTypeToUse) {
            ModelType.LSTM -> {
                activeInterpreter = lstmInterpreter
                modelNameForLog = "LSTM"
            }
            ModelType.LINEAR_REGRESSION_KERAS -> {
                activeInterpreter = lrKerasInterpreter
                modelNameForLog = "Keras LR"
            }
            ModelType.SIMPLE_LINEAR -> {
                Log.i("ElectricityPredictor", "Simple linear prediction 已被移除。")
                return PredictionResult(null, emptyList(), 0.0f, "Simple linear prediction 已被移除")
            }
        }

        if (activeInterpreter == null) {
            Log.e("ElectricityPredictor", "$modelNameForLog interpreter is null. 无法进行预测。")
            return PredictionResult(null, emptyList(), 0.0f, "$modelNameForLog interpreter is null. 无法进行预测。")
        }
        if (scalerScale == null || scalerMin == null){
            Log.e("ElectricityPredictor", "Scaler params are null. Predictions will be inaccurate. 无法进行预测。")
            return PredictionResult(null, emptyList(), 0.0f, "Scaler params are null. 无法进行预测。")
        }

        val dailyPredictions = mutableListOf<DailyPrediction>()
        var tempRemainingBalance = currentBalance
        val today = LocalDateTime.now()
        var currentScaledHistory = initialScaledHistory.toMutableList() // 可变副本

        try {
            var dayOffset = 0
while (tempRemainingBalance > floatComparisonThreshold && dayOffset < 365) { // 最多预测一年，防止死循环
                if (currentScaledHistory.size < DEFAULT_INPUT_LENGTH) {
                    Log.e("ElectricityPredictor", "History size became less than $DEFAULT_INPUT_LENGTH during prediction loop.")
                    break // Should not happen
                }
                // 准备模型输入
                val inputBuffer = ByteBuffer.allocateDirect(DEFAULT_INPUT_LENGTH * Float.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                currentScaledHistory.forEach { inputBuffer.putFloat(it) }
                inputBuffer.rewind()

                val outputBuffer = ByteBuffer.allocateDirect(MODEL_OUTPUT_STEPS * Float.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())

                activeInterpreter.run(inputBuffer, outputBuffer)
                outputBuffer.rewind()

                val predictedScaledUsage = outputBuffer.float // 模型输出的是下一天的归一化用电量
                val predictedRawUsage = inverseScaleUsage(predictedScaledUsage)

                tempRemainingBalance -= predictedRawUsage
                dailyPredictions.add(
                    DailyPrediction(
                        date = today.plusDays(dayOffset.toLong() + 1),
                        predictedUsage = max(0.0f, predictedRawUsage).toDouble(),
                        remainingBalance = tempRemainingBalance
                    )
                )
                // 更新预测输入，移除最早的记录，添加新的预测结果
                currentScaledHistory.removeAt(0)
                currentScaledHistory.add(scaleUsage(predictedRawUsage.toFloat()))  // 将原始的预测用电量转换为归一化值

                if (tempRemainingBalance <= floatComparisonThreshold) {
    tempRemainingBalance = 0.0
    break
}
            }

            val historicalDailyUsage = calculateHistoricalDailyUsage(sortedRecords) // 用于置信度和可能的耗尽估算
            var daysUntilEmptyByModel: Int? = null
            var balanceForDaysCalc = currentBalance
            var days = 0
            var allPredictedUsageIsZero = true
            for (prediction in dailyPredictions) {
                if (prediction.predictedUsage > floatComparisonThreshold) {
                    allPredictedUsageIsZero = false
                }
                if (balanceForDaysCalc >= prediction.predictedUsage) {
                    balanceForDaysCalc -= prediction.predictedUsage
                    days++
                } else {
                    if (prediction.predictedUsage > floatComparisonThreshold) {
                        days += (balanceForDaysCalc / prediction.predictedUsage).toInt()
                    }
                    balanceForDaysCalc = 0.0
                    break
                }
            }


            if (days == dailyPredictions.size && balanceForDaysCalc > floatComparisonThreshold) {
                val lastPredictedUsage = dailyPredictions.lastOrNull()?.predictedUsage ?: historicalDailyUsage
                if (lastPredictedUsage > floatComparisonThreshold) {
                    days += (balanceForDaysCalc / lastPredictedUsage).toInt()
                } else if (historicalDailyUsage > floatComparisonThreshold){
                    days += (balanceForDaysCalc / historicalDailyUsage).toInt()
                } else {

                }
            }
            daysUntilEmptyByModel = if (allPredictedUsageIsZero && currentBalance > floatComparisonThreshold) null else days


            return PredictionResult(
                daysUntilEmpty = daysUntilEmptyByModel,
                predictions = dailyPredictions,
                confidence = calculateConfidence(dailyPredictions, historicalDailyUsage),
                error = null
            )

        }finally {

        }
    }

    private fun calculateHistoricalDailyUsage(records: List<ElectricityRecord>): Double {
        if (records.size < 2) return 0.0
        val sortedForCalc = if (records.first().timestamp.isAfter(records.last().timestamp)) {
            records.sortedBy { it.timestamp }
        } else { records }

        val totalNetUsage = sortedForCalc.map { it.change }.sum().toDouble()
        val firstTimestamp = sortedForCalc.first().timestamp
        val lastTimestamp = sortedForCalc.last().timestamp
        val daysBetween = ChronoUnit.DAYS.between(firstTimestamp, lastTimestamp).toDouble()

        return if (daysBetween > 0 && totalNetUsage > 0) {
            totalNetUsage / daysBetween
        } else if (sortedForCalc.size == 1 && sortedForCalc.first().change > 0) {
            sortedForCalc.first().change.toDouble()
        }
        else { 0.0 }
    }

    private fun calculateConfidence(modelPredictions: List<DailyPrediction>, historicalDailyUsage: Double): Float {
        if (modelPredictions.isEmpty()) return 0.0f
        val avgModelPredictedUsage = modelPredictions.map { it.predictedUsage }.average()
        if (historicalDailyUsage <= floatComparisonThreshold) {
            return if (kotlin.math.abs(avgModelPredictedUsage) <= floatComparisonThreshold) 0.9f else 0.1f
        }
        val difference = kotlin.math.abs(avgModelPredictedUsage - historicalDailyUsage)
        val normalizedDifference = difference / historicalDailyUsage
        return (1.0f - normalizedDifference.toFloat()).coerceIn(0.0f, 1.0f)
    }

    fun close() {
        lstmInterpreter?.close()
        lstmInterpreter = null
        lrKerasInterpreter?.close()
        lrKerasInterpreter = null
        Log.i("ElectricityPredictor", "All TFLite interpreters closed.")
    }
}

data class PredictionResult(
    val daysUntilEmpty: Int?,
    val predictions: List<DailyPrediction>,
    val confidence: Float,
    val error: String?
)

data class DailyPrediction(
    val date: LocalDateTime,
    val predictedUsage: Double,
    val remainingBalance: Double
)
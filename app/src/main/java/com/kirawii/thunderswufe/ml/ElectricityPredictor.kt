package com.kirawii.thunderswufe.ml // 确保包名正确

import android.content.Context
import android.util.Log
import com.kirawii.thunderswufe.data.database.ElectricityRecord // 假设路径
import org.json.JSONObject // 用于解析JSON
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
    SIMPLE_LINEAR // 代表简单回退预测
}

class ElectricityPredictor(private val context: Context) {
    private var lstmInterpreter: Interpreter? = null
    private var lrKerasInterpreter: Interpreter? = null

    // 模型文件名常量
    companion object {
        private const val LSTM_MODEL_FILE = "lstm_model.tflite"
        private const val LR_KERAS_MODEL_FILE = "linear_regression_keras.tflite"
        private const val SCALER_PARAMS_FILE = "scaler_params.json"
        private const val DEFAULT_INPUT_LENGTH = 7 // 与Python训练时一致
        // outputLength 对于单步预测模型通常是1，然后滚动预测
        // 如果你的TFLite模型设计为一次输出多天，这里才需要大于1
        // 假设你的模型是单步预测（预测下一天）
        private const val MODEL_OUTPUT_STEPS = 1 // LSTM 和 LR Keras 都是预测未来1天
        private const val PREDICTION_HORIZON_DAYS = 3 // 我们希望最终展示未来多少天的预测
    }

    // Scaler 参数
    private var scalerScale: FloatArray? = null
    private var scalerMin: FloatArray? = null

    private val floatComparisonThreshold = 1e-6f // 使用 Float

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
            scalerScale = null //确保在失败时它们是null
            scalerMin = null
        }
    }

    // --- 归一化和反归一化 ---
    private fun scaleUsage(rawValue: Float): Float {
        if (scalerScale == null || scalerMin == null || scalerScale!!.isEmpty()) {
            Log.w("ElectricityPredictor", "Scaler not initialized, returning raw value for scaling.")
            return rawValue
        }
        // X_scaled = X * scale_ + min_
        return rawValue * scalerScale!![0] + scalerMin!![0]
    }

    private fun inverseScaleUsage(scaledValue: Float): Float {
        if (scalerScale == null || scalerMin == null || scalerScale!!.isEmpty() || scalerScale!![0] == 0f) {
            Log.w("ElectricityPredictor", "Scaler not initialized or scale is zero, returning scaled value for inverse scaling.")
            return scaledValue
        }
        // X = (X_scaled - min_) / scale_
        return (scaledValue - scalerMin!![0]) / scalerScale!![0]
    }


    fun predictFutureUsage(
        records: List<ElectricityRecord>,
        modelTypeToUse: ModelType = ModelType.LSTM // 默认使用 LSTM
    ): PredictionResult {
        if (records.isEmpty()) {
            return PredictionResult(null, emptyList(), 0.0f, "没有历史数据")
        }

        val sortedRecords = records.sortedBy { it.timestamp } // 最旧的在前
        val currentBalance = sortedRecords.last().balance

        if (sortedRecords.size < DEFAULT_INPUT_LENGTH) {
            Log.w("ElectricityPredictor", "历史数据不足 (需要 $DEFAULT_INPUT_LENGTH, 现有 ${sortedRecords.size})，尝试简单预测。")
            return if (sortedRecords.size >= 2) {
                val historicalDailyUsage = calculateHistoricalDailyUsage(sortedRecords)
                simpleLinearPrediction(currentBalance, historicalDailyUsage)
            } else {
                PredictionResult(null, emptyList(), 0.0f, "历史数据不足以进行任何预测")
            }
        }

        // 准备滚动预测的初始序列 (取最新的 `DEFAULT_INPUT_LENGTH` 条记录的用电量，并归一化)
        // 假设 ElectricityRecord.change 是每日原始正消耗量
        val initialRawHistory = sortedRecords
            .takeLast(DEFAULT_INPUT_LENGTH)
            .map { it.change.toFloat() }

        if (initialRawHistory.size < DEFAULT_INPUT_LENGTH) {
            // This should not happen if sortedRecords.size >= DEFAULT_INPUT_LENGTH check passed
            Log.e("ElectricityPredictor", "Logic error: Not enough data for initial history after filtering.")
            return PredictionResult(null, emptyList(), 0.0f, "内部错误：准备历史数据失败")
        }
        val initialScaledHistory = initialRawHistory.map { scaleUsage(it) }.toMutableList()


        // 选择模型进行预测
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
                Log.i("ElectricityPredictor", "Using simple linear prediction as requested.")
                val historicalDailyUsage = calculateHistoricalDailyUsage(sortedRecords)
                return simpleLinearPrediction(currentBalance, historicalDailyUsage)
            }
        }

        if (activeInterpreter == null) {
            Log.e("ElectricityPredictor", "$modelNameForLog interpreter is null. Falling back to simple linear prediction.")
            val historicalDailyUsage = calculateHistoricalDailyUsage(sortedRecords)
            return simpleLinearPrediction(currentBalance, historicalDailyUsage)
        }
        if (scalerScale == null || scalerMin == null){
            Log.e("ElectricityPredictor", "Scaler params are null. Predictions will be inaccurate. Falling back to simple.")
            val historicalDailyUsage = calculateHistoricalDailyUsage(sortedRecords)
            return simpleLinearPrediction(currentBalance, historicalDailyUsage)
        }


        // --- 执行滚动预测 ---
        val dailyPredictions = mutableListOf<DailyPrediction>()
        var tempRemainingBalance = currentBalance
        val today = LocalDateTime.now()
        var currentScaledHistory = initialScaledHistory.toMutableList() // 可变副本

        try {
            for (dayOffset in 0 until PREDICTION_HORIZON_DAYS) { // 预测未来 PREDICTION_HORIZON_DAYS 天
                if (currentScaledHistory.size < DEFAULT_INPUT_LENGTH) {
                    Log.e("ElectricityPredictor", "History size became less than $DEFAULT_INPUT_LENGTH during prediction loop.")
                    break // Should not happen
                }
                // 准备模型输入
                val inputBuffer = ByteBuffer.allocateDirect(DEFAULT_INPUT_LENGTH * Float.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                currentScaledHistory.forEach { inputBuffer.putFloat(it) }
                inputBuffer.rewind()

                // LSTM 输入形状是 [1, DEFAULT_INPUT_LENGTH, 1]
                // Keras LR TFLite 输入形状是 [1, DEFAULT_INPUT_LENGTH]
                // ByteBuffer 的内容是一样的，但 TFLite Interpreter 的配置会处理形状。
                // 这里假设两个模型 TFLite 转换时，其签名能接受扁平化的 [DEFAULT_INPUT_LENGTH] 序列输入
                // (TFLite converter often flattens the batch dim if it's 1 for single sample inference)
                // 如果 LSTM 需要 [1, seq, 1] 严格输入，这里 inputBuffer 的填充需要调整，或者模型转换时指定固定batch=1.

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

                // 更新历史序列以进行下一次迭代
                if (currentScaledHistory.isNotEmpty()) {
                    currentScaledHistory.removeAt(0)
                }
                currentScaledHistory.add(predictedScaledUsage)

                if (tempRemainingBalance <= floatComparisonThreshold) {
                    // 如果余额已耗尽（或接近耗尽），提前结束预测更多天
                    tempRemainingBalance = 0.0 // 确保为0
                    // 如果需要，填充剩余的预测天数为0用量
                    for (remainingDayOffset in (dayOffset + 1) until PREDICTION_HORIZON_DAYS) {
                        dailyPredictions.add(
                            DailyPrediction(
                                date = today.plusDays(remainingDayOffset.toLong() + 1),
                                predictedUsage = 0.0,
                                remainingBalance = 0.0
                            )
                        )
                    }
                    break
                }
            }

            // 计算预计耗尽天数 (基于滚动预测的结果)
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

        } catch (e: Exception) {
            Log.e("ElectricityPredictor", "Error running TFLite inference with $modelNameForLog: ${e.message}", e)
            val historicalDailyUsage = calculateHistoricalDailyUsage(sortedRecords)
            return simpleLinearPrediction(currentBalance, historicalDailyUsage, "模型($modelNameForLog)预测失败: ${e.localizedMessage}")
        }
    }

    private fun calculateHistoricalDailyUsage(records: List<ElectricityRecord>): Double {
        if (records.size < 2) return 0.0
        val sortedForCalc = if (records.first().timestamp.isAfter(records.last().timestamp)) {
            records.sortedBy { it.timestamp }
        } else { records }

        val totalNetUsage = sortedForCalc.map { it.change }.sum().toDouble() // 假设 change 是每日正消耗
        val firstTimestamp = sortedForCalc.first().timestamp
        val lastTimestamp = sortedForCalc.last().timestamp
        val daysBetween = ChronoUnit.DAYS.between(firstTimestamp, lastTimestamp).toDouble()

        return if (daysBetween > 0 && totalNetUsage > 0) {
            totalNetUsage / daysBetween
        } else if (sortedForCalc.size == 1 && sortedForCalc.first().change > 0) {
            sortedForCalc.first().change.toDouble() // 只有一天记录，用当天的
        }
        else { 0.0 }
    }

    private fun simpleLinearPrediction(
        currentBalance: Double,
        historicalDailyUsage: Double,
        customError: String? = null
    ): PredictionResult {
        // ... (简单线性预测逻辑基本不变, 可以把outputLength 改为 PREDICTION_HORIZON_DAYS) ...
        if (historicalDailyUsage <= floatComparisonThreshold && currentBalance > floatComparisonThreshold) {
            return PredictionResult( null,
                List(PREDICTION_HORIZON_DAYS) { i -> DailyPrediction(LocalDateTime.now().plusDays(i.toLong() + 1), 0.0, currentBalance)},
                0.5f, customError ?: "日均用电为零但有余额"
            )
        }
        if (historicalDailyUsage <= floatComparisonThreshold && currentBalance <= floatComparisonThreshold) {
            return PredictionResult(0, emptyList(),1.0f, customError)
        }

        val dailyPredictions = mutableListOf<DailyPrediction>()
        var tempRemainingBalance = currentBalance
        val today = LocalDateTime.now()

        for (i in 0 until PREDICTION_HORIZON_DAYS) {
            val predictedUsageForDay = historicalDailyUsage
            tempRemainingBalance -= predictedUsageForDay
            if (tempRemainingBalance < 0) tempRemainingBalance = 0.0 // 余额不应为负
            dailyPredictions.add(DailyPrediction(today.plusDays(i.toLong() + 1), max(0.0, predictedUsageForDay), tempRemainingBalance))
            if (tempRemainingBalance == 0.0) break // 提前耗尽
        }
        // 如果预测天数内未耗尽，补齐剩余天数的预测
        if (dailyPredictions.size < PREDICTION_HORIZON_DAYS && tempRemainingBalance == 0.0) {
            for (i in dailyPredictions.size until PREDICTION_HORIZON_DAYS) {
                dailyPredictions.add(DailyPrediction(today.plusDays(i.toLong() + 1), 0.0, 0.0))
            }
        }


        val daysUntilEmpty = if (historicalDailyUsage > floatComparisonThreshold) (currentBalance / historicalDailyUsage).toInt() else null
        return PredictionResult(daysUntilEmpty, dailyPredictions, 0.7f, customError)
    }

    private fun calculateConfidence(modelPredictions: List<DailyPrediction>, historicalDailyUsage: Double): Float {
        // ... (置信度计算逻辑基本不变) ...
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

// --- 数据类 (保持不变) ---
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
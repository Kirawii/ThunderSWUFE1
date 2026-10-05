package com.kirawii.thunderswufe.ml

import android.content.Context
import com.kirawii.thunderswufe.data.database.ElectricityRecord
import java.time.LocalDateTime
import kotlin.math.ceil

enum class ModelType { PREVIOUS_DAY, EXPONENTIAL_SMOOTHING }

/** Causal, device-local forecasting; no private training data or stale fitted model is shipped. */
class ElectricityPredictor {
    companion object {
        private const val MIN_HISTORY_DAYS = 3
        private const val MAX_FORECAST_DAYS = 365
        private const val ALPHA = 0.8
        private const val EPSILON = 1e-6
    }

    constructor(@Suppress("UNUSED_PARAMETER") context: Context)
    internal constructor(@Suppress("UNUSED_PARAMETER") modelJson: String?)

    fun predictFutureUsage(
        records: List<ElectricityRecord>,
        modelTypeToUse: ModelType = ModelType.PREVIOUS_DAY
    ): PredictionResult {
        if (records.isEmpty()) return PredictionResult(null, emptyList(), 0f, "没有历史数据")
        val sorted = records.sortedBy { it.timestamp }
        val today = LocalDateTime.now().toLocalDate()
        val dailyUsage = sorted.groupBy { it.timestamp.toLocalDate() }.toSortedMap()
            .filterKeys { it.isBefore(today) }.values
            .map { rows -> rows.sumOf { it.change.coerceAtLeast(0.0) } }
        if (dailyUsage.size < MIN_HISTORY_DAYS) {
            return PredictionResult(null, emptyList(), 0f,
                "至少需要 $MIN_HISTORY_DAYS 天历史数据，当前只有 ${dailyUsage.size} 天")
        }

        val history = dailyUsage.takeLast(14).toMutableList()
        val upperBound = robustUpperBound(history)
        var remaining = sorted.last().balance.coerceAtLeast(0.0)
        val forecasts = mutableListOf<DailyPrediction>()
        repeat(MAX_FORECAST_DAYS) { offset ->
            if (remaining <= EPSILON) return@repeat
            val usage = forecast(history, modelTypeToUse).coerceIn(0.0, upperBound)
            remaining = (remaining - usage).coerceAtLeast(0.0)
            forecasts += DailyPrediction(today.plusDays(offset.toLong() + 1).atStartOfDay(), usage, remaining)
            history += usage
            if (history.size > 14) history.removeAt(0)
        }
        val average = forecasts.map { it.predictedUsage }.filter { it > EPSILON }
            .takeIf { it.isNotEmpty() }?.average()
        val daysUntilEmpty = when {
            sorted.last().balance <= EPSILON -> 0
            remaining <= EPSILON -> forecasts.indexOfFirst { it.remainingBalance <= EPSILON } + 1
            average != null -> ceil(sorted.last().balance / average).toInt()
            else -> null
        }
        return PredictionResult(daysUntilEmpty, forecasts,
            backtestReliability(dailyUsage, modelTypeToUse), null)
    }

    private fun forecast(history: List<Double>, type: ModelType): Double = when (type) {
        ModelType.PREVIOUS_DAY -> history.last()
        ModelType.EXPONENTIAL_SMOOTHING -> {
            var estimate = history.first()
            history.drop(1).forEach { estimate = ALPHA * it + (1.0 - ALPHA) * estimate }
            estimate
        }
    }

    private fun backtestReliability(values: List<Double>, type: ModelType): Float {
        if (values.size < MIN_HISTORY_DAYS + 1) return 0f
        val errors = (MIN_HISTORY_DAYS until values.size).map { index ->
            kotlin.math.abs(values[index] - forecast(values.subList(0, index).takeLast(14), type))
        }.takeLast(28)
        val scale = values.takeLast(28).average().coerceAtLeast(EPSILON)
        return (1.0 - errors.average() / scale).coerceIn(0.0, 1.0).toFloat()
    }

    private fun robustUpperBound(values: List<Double>): Double {
        val sorted = values.sorted()
        val median = sorted[sorted.size / 2]
        val deviations = values.map { kotlin.math.abs(it - median) }.sorted()
        val mad = deviations[deviations.size / 2]
        return (median + 6.0 * mad).coerceAtLeast(values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    }
}

data class PredictionResult(val daysUntilEmpty: Int?, val predictions: List<DailyPrediction>,
    /** Recent causal backtest score; a stability indicator, not a confidence interval. */
    val confidence: Float, val error: String?)

data class DailyPrediction(val date: LocalDateTime, val predictedUsage: Double, val remainingBalance: Double)

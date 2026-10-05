package com.kirawii.thunderswufe.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import java.util.Locale

/** Straight segments preserve the observed values without interpolation overshoot. */
@Composable
fun EnergyChart(values: List<Double>, labels: List<String>, title: String,
    modifier: Modifier = Modifier, forecast: Boolean = false) {
    val color = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    var selected by remember(values, labels) { mutableStateOf(values.lastIndex) }
    val safe = values.map { if (it.isFinite()) it.coerceAtLeast(0.0) else 0.0 }
    val maximum = (safe.maxOrNull() ?: 0.0).coerceAtLeast(1.0) * 1.12
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (safe.isEmpty()) {
            Text("暂无记录 · 获取数据后显示趋势", modifier = Modifier.padding(vertical = 48.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(title, style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format(Locale.CHINA, "%.2f 度", safe[selected.coerceIn(safe.indices)]),
                        style = MaterialTheme.typography.headlineSmall, color = color)
                }
                Text(labels.getOrElse(selected) { "" }, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.width(40.dp).height(180.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    listOf(maximum, maximum / 2, 0.0).forEach {
                        Text(String.format(Locale.CHINA, "%.0f", it), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Canvas(Modifier.weight(1f).height(180.dp)
                    .semantics { contentDescription = "$title，${safe.size} 个数据点，${labels.getOrElse(selected) { "" }}，${safe[selected.coerceIn(safe.indices)]} 度，点击查看日期和读数" }
                    .pointerInput(safe) {
                        detectTapGestures { position ->
                            selected = ((position.x / size.width) * safe.lastIndex).roundToInt().coerceIn(safe.indices)
                        }
                    }) {
                    val inset = 6.dp.toPx()
                    val plotHeight = size.height - 2 * inset
                    fun point(index: Int) = Offset(
                        if (safe.size == 1) size.width / 2 else index * size.width / safe.lastIndex,
                        inset + plotHeight * (1 - safe[index] / maximum).toFloat())
                    repeat(3) { tick ->
                        val y = inset + plotHeight * tick / 2
                        drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
                    }
                    val line = Path().apply {
                        val first = point(0); moveTo(first.x, first.y)
                        for (index in 1..safe.lastIndex) { val p = point(index); lineTo(p.x, p.y) }
                    }
                    if (!forecast && safe.size > 1) {
                        val area = Path().apply {
                            addPath(line); lineTo(size.width, size.height); lineTo(0f, size.height); close()
                        }
                        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.01f))))
                    }
                    drawPath(line, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round,
                        join = StrokeJoin.Round, pathEffect = if (forecast)
                            PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())) else null))
                    val p = point(selected.coerceIn(safe.indices))
                    drawLine(color.copy(alpha = 0.3f), Offset(p.x, inset), Offset(p.x, size.height), 1.dp.toPx())
                    drawCircle(color.copy(alpha = 0.18f), 8.dp.toPx(), p)
                    drawCircle(color, 4.dp.toPx(), p)
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0, labels.lastIndex / 2, labels.lastIndex).distinct().forEach { index ->
                    Text(labels.getOrElse(index) { "" }, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(if (forecast) "虚线为预测 · 点击曲线查看每日估计" else "点击曲线查看读数 · 单位：度（kWh）",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnergyChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(option) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer))
        }
    }
}

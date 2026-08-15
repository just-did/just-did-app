package com.zhouyp.justdid.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun PieChartView(
    usedBytes: Long,
    totalBytes: Long,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usedColor = Color(0xFF008CFF)
    val freeColor = Color(0xFFE0E0E0)
    val usedPercent = if (totalBytes <= 0) {
        0f
    } else {
        (usedBytes.toFloat() / totalBytes * 100f).coerceIn(0f, 100f)
    }
    val freePercent = 100f - usedPercent

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(240.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 42.dp.toPx())
                val radius = (size.minDimension - stroke.width) / 2
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
                val arcSize = Size(radius * 2, radius * 2)

                // 占用扇区
                drawArc(
                    color = usedColor,
                    startAngle = -90f,
                    sweepAngle = 360f * usedPercent / 100f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )

                // 空闲扇区
                drawArc(
                    color = freeColor,
                    startAngle = -90f + 360f * usedPercent / 100f,
                    sweepAngle = 360f * freePercent / 100f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }

            Text(
                text = "${formatBytes(usedBytes)} / ${formatBytes(totalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "刷新缓存占用",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 图例
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = freeColor, label = "空闲 ${freePercent.toInt()}%")
            Spacer(modifier = Modifier.width(24.dp))
            LegendItem(color = usedColor, label = "占用 ${usedPercent.toInt()}%")
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1L shl 30 -> formatOneDecimal(bytes / 1073741824.0) + " GB"
        bytes >= 1L shl 20 -> formatOneDecimal(bytes / 1048576.0) + " MB"
        bytes >= 1L shl 10 -> formatOneDecimal(bytes / 1024.0) + " KB"
        else -> "$bytes B"
    }
}

private fun formatOneDecimal(value: Double): String {
    val formatted = String.format(Locale.US, "%.1f", value)
    return if (formatted.endsWith(".0")) formatted.dropLast(2) else formatted
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = color
        ) {}
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

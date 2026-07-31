package com.zhouyp.justdid.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun PieChartView(
    usedPercent: Float,
    modifier: Modifier = Modifier
) {
    val usedColor = Color(0xFF008CFF)
    val freeColor = Color(0xFFE0E0E0)
    val freePercent = 100f - usedPercent

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier.size(240.dp)
        ) {
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

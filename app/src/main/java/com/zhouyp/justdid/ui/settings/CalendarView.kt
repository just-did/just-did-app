package com.zhouyp.justdid.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDates: Set<LocalDate>,
    reportStatus: Map<LocalDate, Int>,
    onDateClick: (LocalDate) -> Unit
) {
    val firstOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val today = LocalDate.now()

    val dayOfWeek = firstOfMonth.dayOfWeek.value
    val offset = (dayOfWeek - DayOfWeek.MONDAY.value) % 7

    val totalCells = 42
    val allDates = (0 until totalCells).map { cellIndex ->
        val day = cellIndex - offset + 1
        if (day < 1) {
            firstOfMonth.minusDays((1 - day).toLong())
        } else if (day > daysInMonth) {
            firstOfMonth.plusDays((day - 1).toLong())
        } else {
            firstOfMonth.withDayOfMonth(day)
        }
    }

    Column {
        WeekdayHeader()

        val rows = allDates.chunked(7)
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (date in row) {
                    val isFutureDisabled = date > today
                    val isCurrentMonth = YearMonth.from(date) == currentMonth
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(29.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DateCell(
                            date = date,
                            isCurrentMonth = isCurrentMonth,
                            isSelected = date in selectedDates,
                            isDisabled = isFutureDisabled,
                            dotStatus = if (isCurrentMonth) reportStatus[date] else null,
                            onClick = {
                                if (isCurrentMonth) {
                                    onDateClick(date)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (day in weekdays) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    isDisabled: Boolean,
    dotStatus: Int?,
    onClick: () -> Unit
) {
    val textColor = when {
        isSelected -> Color.White
        isDisabled || !isCurrentMonth ->
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val dotColor = when (dotStatus) {
        1 -> Color(0xFF4CAF50)
        0 -> Color(0xFFFF9800)
        else -> null
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .clickable(enabled = !isDisabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF008CFF))
            )
        }
        Row(verticalAlignment = Alignment.Top) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Text(
                text = "${date.dayOfMonth}",
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

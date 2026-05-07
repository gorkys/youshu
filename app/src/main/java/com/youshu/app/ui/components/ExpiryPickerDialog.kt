package com.youshu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.util.DateUtil
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpiryPickerDialog(
    selectedDateMillis: Long?,
    onDismissRequest: () -> Unit,
    onClear: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zoneId) }
    val initialDate = remember(selectedDateMillis) {
        selectedDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
        } ?: today
    }
    var selectedYear by remember(initialDate) { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember(initialDate) { mutableIntStateOf(initialDate.monthValue) }
    var selectedDay by remember(initialDate) { mutableIntStateOf(initialDate.dayOfMonth) }
    var selectedPanel by remember { mutableStateOf(ExpiryPanel.MONTH) }

    val yearRange = remember(today.year) { (today.year..today.year + 8).toList() }
    val currentYearMonth = remember(selectedYear, selectedMonth) { YearMonth.of(selectedYear, selectedMonth) }
    val maxDay = currentYearMonth.lengthOfMonth()
    if (selectedDay > maxDay) {
        selectedDay = maxDay
    }

    AppDialog(
        title = "选择有效期",
        subtitle = "先选年份，再直观切换月份和日期。",
        onDismissRequest = onDismissRequest,
        confirmText = "确定",
        secondaryText = "清空",
        onSecondary = onClear,
        onConfirm = {
            val millis = LocalDate.of(selectedYear, selectedMonth, selectedDay)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
            onConfirm(millis)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpiryTab(
                text = "${selectedYear}年",
                selected = selectedPanel == ExpiryPanel.YEAR,
                onClick = { selectedPanel = ExpiryPanel.YEAR },
                modifier = Modifier.weight(1f)
            )
            ExpiryTab(
                text = "${selectedMonth}月",
                selected = selectedPanel == ExpiryPanel.MONTH,
                onClick = { selectedPanel = ExpiryPanel.MONTH },
                modifier = Modifier.weight(1f)
            )
            ExpiryTab(
                text = "${selectedDay}日",
                selected = selectedPanel == ExpiryPanel.DAY,
                onClick = { selectedPanel = ExpiryPanel.DAY },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = DateUtil.formatDate(
                LocalDate.of(selectedYear, selectedMonth, selectedDay)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
            ),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedPanel) {
                ExpiryPanel.YEAR -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        yearRange.forEach { year ->
                            ExpiryOptionChip(
                                text = "${year}年",
                                selected = selectedYear == year,
                                onClick = {
                                    selectedYear = year
                                    selectedPanel = ExpiryPanel.MONTH
                                }
                            )
                        }
                    }
                }

                ExpiryPanel.MONTH -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..12).forEach { month ->
                            ExpiryOptionChip(
                                text = "${month}月",
                                selected = selectedMonth == month,
                                onClick = {
                                    selectedMonth = month
                                    selectedPanel = ExpiryPanel.DAY
                                }
                            )
                        }
                    }
                }

                ExpiryPanel.DAY -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..maxDay).forEach { day ->
                            ExpiryOptionChip(
                                text = "${day}日",
                                selected = selectedDay == day,
                                onClick = { selectedDay = day }
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class ExpiryPanel {
    YEAR,
    MONTH,
    DAY
}

@Composable
private fun ExpiryTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = if (selected) OrangeStart.copy(alpha = 0.12f) else Color(0xFFF8F6FC),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) OrangeStart else TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ExpiryOptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = if (selected) OrangeStart.copy(alpha = 0.14f) else Color(0xFFF8F6FC),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(OrangeStart, CircleShape)
            )
        }
        Text(
            text = text,
            color = if (selected) OrangeStart else TextHint,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(start = if (selected) 6.dp else 0.dp)
        )
    }
}

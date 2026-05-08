package com.youshu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.util.DateUtil
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

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
        subtitle = "按年、月、日分步选择，布局固定更直观。",
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
                text = selectedDay.toString(),
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

        when (selectedPanel) {
            ExpiryPanel.YEAR -> {
                FixedGridOptions(
                    columns = 3,
                    items = yearRange,
                    key = { it },
                    label = { it.toString() },
                    selected = { selectedYear == it },
                    onSelect = {
                        selectedYear = it
                        selectedPanel = ExpiryPanel.MONTH
                    }
                )
            }

            ExpiryPanel.MONTH -> {
                FixedGridOptions(
                    columns = 4,
                    items = (1..12).toList(),
                    key = { it },
                    label = { "${it}月" },
                    selected = { selectedMonth == it },
                    onSelect = {
                        selectedMonth = it
                        selectedPanel = ExpiryPanel.DAY
                    }
                )
            }

            ExpiryPanel.DAY -> {
                FixedGridOptions(
                    columns = 7,
                    items = (1..maxDay).toList(),
                    key = { it },
                    label = { it.toString() },
                    selected = { selectedDay == it },
                    onSelect = { selectedDay = it }
                )
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
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                color = if (selected) OrangeStart.copy(alpha = 0.12f) else Color(0xFFF8F6FC),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = OrangeStart.copy(alpha = 0.16f)
                ),
                onClick = onClick
            )
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
private fun <T> FixedGridOptions(
    columns: Int,
    items: List<T>,
    key: (T) -> Any,
    label: (T) -> String,
    selected: (T) -> Boolean,
    onSelect: (T) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = true
    ) {
        items(items, key = key) { item ->
            ExpiryGridCell(
                text = label(item),
                selected = selected(item),
                onClick = { onSelect(item) }
            )
        }
    }
}

@Composable
private fun ExpiryGridCell(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                color = if (selected) OrangeStart.copy(alpha = 0.14f) else Color(0xFFF8F6FC),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = OrangeStart.copy(alpha = 0.16f)
                ),
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) OrangeStart else TextHint,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

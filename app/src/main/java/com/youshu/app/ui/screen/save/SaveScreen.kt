package com.youshu.app.ui.screen.save

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.youshu.app.ui.components.AppDialog
import com.youshu.app.ui.components.AppSurfaceCard
import com.youshu.app.ui.components.EditorInputBox
import com.youshu.app.ui.components.EditorSectionLabel
import com.youshu.app.ui.components.EditorSelectionChip
import com.youshu.app.ui.components.GradientButton
import com.youshu.app.ui.components.QuantityStepper
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.SaveViewModel
import com.youshu.app.util.DateUtil

private data class ExpiryQuickOption(val label: String, val timestamp: Long)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SaveScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SaveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val leafLocations = locations.filter { it.parentId != null }

    var showAdvanced by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.expireTime)
    val expiryQuickOptions = remember {
        listOf(
            ExpiryQuickOption("7天", DateUtil.daysFromNow(7)),
            ExpiryQuickOption("1个月", DateUtil.monthsFromNow(1)),
            ExpiryQuickOption("3个月", DateUtil.monthsFromNow(3)),
            ExpiryQuickOption("6个月", DateUtil.monthsFromNow(6)),
            ExpiryQuickOption("12个月", DateUtil.monthsFromNow(12))
        )
    }

    LaunchedEffect(imageUri) {
        viewModel.initFromPhoto(context, imageUri, null, null)
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onSaved()
            viewModel.reset()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.34f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.28f),
                            Color(0xCCFFF9F1),
                            Color(0xEEFFF9F2)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "快速录入",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(48.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AppSurfaceCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 96.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 42.dp, height = 4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFE5DFEC))
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        AsyncImage(
                            model = imageUri,
                            contentDescription = "图片预览",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(188.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                        EditorSectionLabel(label = "物品名称", tag = "AI识别")
                        Spacer(modifier = Modifier.height(8.dp))
                        EditorInputBox(
                            value = state.name,
                            onValueChange = viewModel::updateName,
                            placeholder = "请输入物品名称"
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        EditorSectionLabel(label = "分类", tag = "AI推荐")
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { category ->
                                EditorSelectionChip(
                                    text = category.name,
                                    selected = state.categoryId == category.id,
                                    onClick = {
                                        viewModel.updateCategory(
                                            if (state.categoryId == category.id) null else category.id
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        EditorSectionLabel(label = "存放位置")
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            leafLocations.forEach { location ->
                                EditorSelectionChip(
                                    text = location.name,
                                    selected = state.locationId == location.id,
                                    onClick = {
                                        viewModel.updateLocation(
                                            if (state.locationId == location.id) null else location.id
                                        )
                                    },
                                    icon = Icons.Default.LocationOn
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvanced = !showAdvanced }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "更多信息（选填）",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }

                        AnimatedVisibility(
                            visible = showAdvanced,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                EditorSectionLabel(label = "数量")
                                Spacer(modifier = Modifier.height(8.dp))
                                QuantityStepper(
                                    quantity = state.quantity,
                                    unit = state.unit,
                                    onDecrease = { viewModel.updateQuantity(state.quantity - 1) },
                                    onIncrease = { viewModel.updateQuantity(state.quantity + 1) }
                                )

                                Spacer(modifier = Modifier.height(18.dp))
                                EditorSectionLabel(label = "有效期")
                                Spacer(modifier = Modifier.height(8.dp))
                                EditorInputBox(
                                    value = state.expireTime?.let(DateUtil::formatDate).orEmpty(),
                                    onValueChange = {},
                                    placeholder = "点击选择日期",
                                    readOnly = true,
                                    modifier = Modifier.clickable { showDatePicker = true },
                                    trailingContent = {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = TextHint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    expiryQuickOptions.forEach { option ->
                                        EditorSelectionChip(
                                            text = option.label,
                                            selected = state.expireTime == option.timestamp,
                                            onClick = { viewModel.updateExpireTime(option.timestamp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))
                                EditorSectionLabel(label = "价格（估算）")
                                Spacer(modifier = Modifier.height(8.dp))
                                EditorInputBox(
                                    value = state.price,
                                    onValueChange = viewModel::updatePrice,
                                    placeholder = "例如 8.90"
                                )

                                Spacer(modifier = Modifier.height(18.dp))
                                EditorSectionLabel(label = "备注")
                                Spacer(modifier = Modifier.height(8.dp))
                                EditorInputBox(
                                    value = state.note,
                                    onValueChange = viewModel::updateNote,
                                    placeholder = "可以补充品牌、规格或使用说明",
                                    singleLine = false,
                                    minHeight = 112
                                )
                            }
                        }
                    }
                }

                GradientButton(
                    text = if (state.isSaving) "保存中…" else "保存",
                    onClick = viewModel::save,
                    enabled = state.name.isNotBlank() && !state.isSaving,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 18.dp)
                )
            }
        }

        if (showDatePicker) {
            AppDialog(
                title = "选择有效期",
                subtitle = "可以切换月份快速定位，也可以使用上面的快捷按钮。",
                onDismissRequest = { showDatePicker = false },
                confirmText = "确定",
                secondaryText = "清空",
                onSecondary = {
                    viewModel.updateExpireTime(null)
                    showDatePicker = false
                },
                onConfirm = {
                    viewModel.updateExpireTime(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

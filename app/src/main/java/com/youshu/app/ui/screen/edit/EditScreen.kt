package com.youshu.app.ui.screen.edit

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.youshu.app.ui.components.AppDecorativeBackground
import com.youshu.app.ui.components.AppDialog
import com.youshu.app.ui.components.AppSurfaceCard
import com.youshu.app.ui.components.EditorInputBox
import com.youshu.app.ui.components.EditorSectionLabel
import com.youshu.app.ui.components.EditorSelectionChip
import com.youshu.app.ui.components.ExpiryPickerDialog
import com.youshu.app.ui.components.GradientButton
import com.youshu.app.ui.components.ItemImageGallery
import com.youshu.app.ui.components.QuantityStepper
import com.youshu.app.ui.screen.save.SavePhotoMode
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.EditViewModel
import com.youshu.app.util.DateUtil

private data class ExpiryQuickOption(val label: String, val timestamp: Long)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    itemId: Long,
    pendingImageUri: Uri?,
    pendingImageUris: List<Uri>,
    pendingPhotoMode: SavePhotoMode?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenCamera: (SavePhotoMode) -> Unit,
    viewModel: EditViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val leafLocations = locations.filter { it.parentId != null }

    var showAdvanced by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    val expiryQuickOptions = remember {
        listOf(
            ExpiryQuickOption("7天", DateUtil.daysFromNow(7)),
            ExpiryQuickOption("1个月", DateUtil.monthsFromNow(1)),
            ExpiryQuickOption("3个月", DateUtil.monthsFromNow(3)),
            ExpiryQuickOption("6个月", DateUtil.monthsFromNow(6)),
            ExpiryQuickOption("12个月", DateUtil.monthsFromNow(12))
        )
    }

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    LaunchedEffect(pendingImageUri, pendingImageUris, pendingPhotoMode) {
        when {
            pendingPhotoMode == SavePhotoMode.REPLACE_PRIMARY && pendingImageUri != null -> {
                viewModel.replacePrimaryPhoto(context, pendingImageUri)
            }

            pendingImageUris.isNotEmpty() -> {
                viewModel.appendPhotos(context, pendingImageUris)
            }

            pendingImageUri != null -> {
                viewModel.appendPhotos(context, listOf(pendingImageUri))
            }
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onSaved()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppDecorativeBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
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
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "编辑物品",
                    fontSize = 22.sp,
                    color = TextPrimary
                )
            }

            AppSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp)
                ) {
                    ItemImageGallery(
                        imagePaths = state.imagePaths,
                        onCapturePrimary = { onOpenCamera(SavePhotoMode.REPLACE_PRIMARY) },
                        onCaptureDetail = { onOpenCamera(SavePhotoMode.APPEND) },
                        onSelectNoImage = null,
                        onRemoveImage = viewModel::removePhoto,
                        onMoveImage = viewModel::movePhoto
                    )

                    Spacer(modifier = Modifier.size(18.dp))
                    EditorSectionLabel(label = "物品名称")
                    Spacer(modifier = Modifier.size(8.dp))
                    EditorInputBox(
                        value = state.name,
                        onValueChange = viewModel::updateName,
                        placeholder = "请输入物品名称"
                    )

                    Spacer(modifier = Modifier.size(16.dp))
                    EditorSectionLabel(label = "分类")
                    Spacer(modifier = Modifier.size(8.dp))
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

                    Spacer(modifier = Modifier.size(16.dp))
                    EditorSectionLabel(label = "存放位置")
                    Spacer(modifier = Modifier.size(8.dp))
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

                    Spacer(modifier = Modifier.size(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvanced = !showAdvanced }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "更多信息",
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
                            Spacer(modifier = Modifier.size(12.dp))
                            EditorSectionLabel(label = "数量")
                            Spacer(modifier = Modifier.size(8.dp))
                            QuantityStepper(
                                quantity = state.quantity,
                                unit = state.unit,
                                onDecrease = { viewModel.updateQuantity(state.quantity - 1) },
                                onIncrease = { viewModel.updateQuantity(state.quantity + 1) }
                            )

                            Spacer(modifier = Modifier.size(18.dp))
                            EditorSectionLabel(label = "有效期")
                            Spacer(modifier = Modifier.size(8.dp))
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
                            Spacer(modifier = Modifier.size(10.dp))
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

                            Spacer(modifier = Modifier.size(18.dp))
                            EditorSectionLabel(label = "价格（估算）")
                            Spacer(modifier = Modifier.size(8.dp))
                            EditorInputBox(
                                value = state.price,
                                onValueChange = viewModel::updatePrice,
                                placeholder = "例如 8.90"
                            )

                            Spacer(modifier = Modifier.size(18.dp))
                            EditorSectionLabel(label = "备注")
                            Spacer(modifier = Modifier.size(8.dp))
                            EditorInputBox(
                                value = state.note,
                                onValueChange = viewModel::updateNote,
                                placeholder = "可以补充物品来源、规格或注意事项",
                                singleLine = false,
                                minHeight = 112
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(24.dp))
                    GradientButton(
                        text = if (state.isSaving) "保存中..." else "保存修改",
                        onClick = viewModel::save,
                        enabled = state.name.isNotBlank() && !state.isSaving
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "修改后会同步更新首页、库房和分类结果。",
                        fontSize = 12.sp,
                        color = TextHint,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        if (showDatePicker) {
            ExpiryPickerDialog(
                selectedDateMillis = state.expireTime,
                onDismissRequest = { showDatePicker = false },
                onClear = {
                    viewModel.updateExpireTime(null)
                    showDatePicker = false
                },
                onConfirm = { selectedMillis ->
                    viewModel.updateExpireTime(selectedMillis)
                    showDatePicker = false
                }
            )
        }
    }
}

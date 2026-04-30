package com.youshu.app.ui.screen.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RestoreFromTrash
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.ui.components.AppDecorativeBackground
import com.youshu.app.ui.components.AppDialog
import com.youshu.app.ui.components.AppSurfaceCard
import com.youshu.app.ui.components.EmptyState
import com.youshu.app.ui.components.SectionHeader
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.StatusExpired
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.TrashViewModel
import com.youshu.app.util.DateUtil

@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val deletedItems by viewModel.deletedItems.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    var pendingDeleteIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    LaunchedEffect(deletedItems) {
        viewModel.pruneSelection(deletedItems.map { it.item.id }.toSet())
    }

    val allItemIds = remember(deletedItems) { deletedItems.map { it.item.id }.toSet() }
    val allSelected = allItemIds.isNotEmpty() && selectedIds.containsAll(allItemIds)
    val selectedCount = selectedIds.size

    Box(modifier = Modifier.fillMaxSize()) {
        AppDecorativeBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "回收站",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "删除后 30 天内可恢复，过期会自动清理",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            if (deletedItems.isEmpty()) {
                EmptyState(
                    title = "回收站是空的",
                    message = "最近删除的物品会先放在这里，30 天内可以恢复。",
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                AppSurfaceCard(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedCount == 0) "未选择项目" else "已选择 $selectedCount 项",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "支持批量恢复与永久删除",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        TrashActionButton(
                            text = if (allSelected) "清空" else "全选",
                            onClick = {
                                if (allSelected) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll(allItemIds)
                                }
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TrashActionButton(
                            text = "批量恢复",
                            modifier = Modifier.weight(1f),
                            enabled = selectedCount > 0,
                            highlight = true,
                            onClick = viewModel::restoreSelected
                        )
                        TrashActionButton(
                            text = "永久删除",
                            modifier = Modifier.weight(1f),
                            enabled = selectedCount > 0,
                            destructive = true,
                            onClick = { pendingDeleteIds = selectedIds.toList() }
                        )
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "最近删除",
                            subtitle = "共 ${deletedItems.size} 项可在 30 天内恢复"
                        )
                    }
                    items(deletedItems, key = { it.item.id }) { itemDetail ->
                        TrashItemCard(
                            itemDetail = itemDetail,
                            selected = itemDetail.item.id in selectedIds,
                            onToggleSelection = { viewModel.toggleSelection(itemDetail.item.id) },
                            onRestore = { viewModel.restore(itemDetail.item.id) },
                            onDelete = { pendingDeleteIds = listOf(itemDetail.item.id) }
                        )
                    }
                }
            }
        }
    }

    if (pendingDeleteIds.isNotEmpty()) {
        val isBatchDelete = pendingDeleteIds.size > 1
        AppDialog(
            title = if (isBatchDelete) "永久删除选中的 ${pendingDeleteIds.size} 项？" else "永久删除这个物品？",
            subtitle = "永久删除后会立即清空回收站记录和对应图片，无法恢复。",
            onDismissRequest = { pendingDeleteIds = emptyList() },
            confirmText = "永久删除",
            dismissText = "取消",
            destructiveConfirm = true,
            onConfirm = {
                viewModel.permanentlyDelete(pendingDeleteIds)
                pendingDeleteIds = emptyList()
            }
        )
    }
}

@Composable
private fun TrashItemCard(
    itemDetail: ItemDetail,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val item = itemDetail.item
    val deletedAt = item.deletedAt ?: System.currentTimeMillis()
    val restoreDeadline = deletedAt + 30L * 24 * 60 * 60 * 1000
    val daysLeft = DateUtil.daysUntil(restoreDeadline).coerceAtLeast(0)
    val shape = RoundedCornerShape(24.dp)

    AppSurfaceCard(
        modifier = Modifier.border(
            width = 1.dp,
            color = if (selected) OrangeStart.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f),
            shape = shape
        ),
        shape = shape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(StatusExpired.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = StatusExpired
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = listOfNotNull(itemDetail.categoryName, itemDetail.locationName)
                        .joinToString(" · ")
                        .ifBlank { "未分类 / 未设置位置" },
                    fontSize = 12.sp,
                    color = TextHint,
                    modifier = Modifier.padding(top = 3.dp)
                )
                Text(
                    text = "删除于 ${DateUtil.formatDateTime(deletedAt)}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = if (daysLeft == 0L) "今天内仍可恢复" else "剩余 $daysLeft 天可恢复",
                    fontSize = 12.sp,
                    color = OrangeStart,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (selected) OrangeStart else Color(0xFFF4F1E7))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleSelection
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TrashActionButton(
                text = "恢复",
                icon = Icons.Default.RestoreFromTrash,
                modifier = Modifier.weight(1f),
                highlight = true,
                onClick = onRestore
            )
            TrashActionButton(
                text = "永久删除",
                icon = Icons.Default.DeleteForever,
                modifier = Modifier.weight(1f),
                destructive = true,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun TrashActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    highlight: Boolean = false,
    destructive: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val background = when {
        !enabled -> Color(0xFFF2EFF7)
        destructive -> StatusExpired.copy(alpha = 0.12f)
        highlight -> OrangeStart.copy(alpha = 0.12f)
        else -> Color(0xFFF7F4EC)
    }
    val contentColor = when {
        !enabled -> TextHint
        destructive -> StatusExpired
        highlight -> OrangeStart
        else -> TextSecondary
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
        }
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

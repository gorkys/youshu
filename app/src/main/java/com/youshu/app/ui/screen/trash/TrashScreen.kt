package com.youshu.app.ui.screen.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.ui.components.AppDecorativeBackground
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
                Column {
                    Text(
                        text = "回收站",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "删除后 30 天内可恢复，超时会自动清理。",
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
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "最近删除",
                            subtitle = "共 ${deletedItems.size} 项可恢复"
                        )
                    }
                    items(deletedItems, key = { it.item.id }) { itemDetail ->
                        TrashItemCard(
                            itemDetail = itemDetail,
                            onRestore = { viewModel.restore(itemDetail.item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrashItemCard(
    itemDetail: ItemDetail,
    onRestore: () -> Unit
) {
    val item = itemDetail.item
    val deletedAt = item.deletedAt ?: System.currentTimeMillis()
    val restoreDeadline = deletedAt + 30L * 24 * 60 * 60 * 1000
    val daysLeft = DateUtil.daysUntil(restoreDeadline).coerceAtLeast(0)

    AppSurfaceCard(
        shape = RoundedCornerShape(24.dp),
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
                    text = listOfNotNull(itemDetail.categoryName, itemDetail.locationName).joinToString(" · ").ifBlank { "未分类 / 未设置位置" },
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
                    text = if (daysLeft == 0L) "今天内仍可恢复" else "剩余 ${daysLeft} 天可恢复",
                    fontSize = 12.sp,
                    color = OrangeStart,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrangeStart.copy(alpha = 0.12f))
                    .clickable(onClick = onRestore)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "恢复",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OrangeStart
                )
            }
        }
    }
}

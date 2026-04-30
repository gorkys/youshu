package com.youshu.app.ui.screen.search

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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.youshu.app.ui.components.AppDecorativeBackground
import com.youshu.app.ui.components.AppSurfaceCard
import com.youshu.app.ui.components.EmptyState
import com.youshu.app.ui.components.ItemCard
import com.youshu.app.ui.components.SearchBar
import com.youshu.app.ui.components.SectionHeader
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.LibraryStatusFilter
import com.youshu.app.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    onNavigateToDetail: (Long, LibraryStatusFilter) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val statusCounts by viewModel.statusCounts.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        AppDecorativeBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 10.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "库房",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "集中查看全部物品、已用完和评价状态。",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )
                SearchBar(
                    value = query,
                    onValueChange = viewModel::updateQuery,
                    placeholder = "搜索物品、分类、位置或备注",
                    showMagicIconWhenEmpty = false
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusCounts.forEach { item ->
                    val selected = selectedFilter == item.filter
                    LibraryFilterChip(
                        filter = item.filter,
                        count = item.count,
                        selected = selected,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectFilter(item.filter) }
                    )
                }
            }

            if (results.isEmpty()) {
                EmptyState(
                    title = "没有找到匹配物品",
                    message = if (query.isBlank()) {
                        "当前筛选状态下还没有物品。"
                    } else {
                        "可以更换关键词，或者切换上面的状态筛选。"
                    },
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 132.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        SectionHeader(
                            title = selectedFilter.label,
                            subtitle = "共 ${results.size} 件物品",
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(results, key = { it.item.id }) { itemDetail ->
                        ItemCard(
                            itemDetail = itemDetail,
                            onClick = { onNavigateToDetail(itemDetail.item.id, selectedFilter) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryFilterChip(
    filter: LibraryStatusFilter,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (filter) {
        LibraryStatusFilter.ALL -> Icons.Default.Inventory2
        LibraryStatusFilter.USED_UP -> Icons.Default.TaskAlt
        LibraryStatusFilter.PENDING_REVIEW,
        LibraryStatusFilter.REVIEWED -> Icons.Default.RateReview
    }
    val iconTint = if (selected) OrangeStart else TextSecondary

    AppSurfaceCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
        containerColor = if (selected) OrangeStart.copy(alpha = 0.1f) else Color.White,
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE65B5B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = filter.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) OrangeStart else TextPrimary
                )
            }
        }
    }
}

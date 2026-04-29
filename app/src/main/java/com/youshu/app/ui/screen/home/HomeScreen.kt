package com.youshu.app.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.youshu.app.ui.components.PillTag
import com.youshu.app.ui.components.SearchBar
import com.youshu.app.ui.components.SectionHeader
import com.youshu.app.ui.theme.OrangeEnd
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.StatusExpired
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSearchCenter: () -> Unit,
    onNavigateToExpiry: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val activeItems by viewModel.activeItems.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    val expiringCount by viewModel.expiringCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }

    val filteredItems = activeItems.filter { itemDetail ->
        val categoryMatched = selectedCategoryId == null || itemDetail.item.categoryId == selectedCategoryId
        val queryMatched = searchQuery.isBlank() || listOf(
            itemDetail.item.name,
            itemDetail.categoryName.orEmpty(),
            itemDetail.locationName.orEmpty(),
            itemDetail.item.note
        ).any { it.contains(searchQuery, ignoreCase = true) }
        categoryMatched && queryMatched
    }

    val sectionTitle = when {
        searchQuery.isNotBlank() -> "搜索结果"
        selectedCategoryId != null -> categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "分类筛选"
        else -> "最近添加"
    }

    val sectionSubtitle = when {
        searchQuery.isNotBlank() -> "根据当前搜索词筛选出的物品"
        selectedCategoryId != null -> "当前分类下共 ${filteredItems.size} 件物品"
        else -> "拍一下、存一下，常用物品一眼找到"
    }

    Box {
        AppDecorativeBackground()

        LazyColumn(
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            item {
                HeroHeader(
                    searchQuery = searchQuery,
                    onOpenSearchCenter = onNavigateToSearchCenter
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OverviewRowCard(
                        title = "即将过期",
                        value = "${expiringCount} 件物品",
                        description = if (expiringCount > 0) "建议优先处理这批快到期的物品" else "当前没有需要提醒的物品",
                        icon = Icons.Default.Notifications,
                        accent = StatusExpired,
                        onClick = onNavigateToExpiry
                    )
                    OverviewRowCard(
                        title = "全部物品",
                        value = "${allItems.size} 件物品",
                        description = "进入库房查看全部、已用完和待评价状态",
                        icon = Icons.Default.Inventory2,
                        accent = OrangeStart,
                        onClick = onNavigateToLibrary
                    )
                }
            }

            if (categories.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        categories.take(8).forEach { category ->
                            val selected = selectedCategoryId == category.id
                            PillTag(
                                text = category.name,
                                modifier = Modifier.clickable {
                                    selectedCategoryId = if (selected) null else category.id
                                },
                                backgroundColor = if (selected) OrangeStart else Color.White,
                                contentColor = if (selected) Color.White else TextHint
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = sectionTitle,
                    subtitle = sectionSubtitle,
                    action = "共 ${filteredItems.size} 件",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }

            if (filteredItems.isEmpty()) {
                item {
                    EmptyState(
                        title = "没有找到匹配物品",
                        message = if (searchQuery.isNotBlank() || selectedCategoryId != null) {
                            "可以更换关键词，或者取消当前筛选条件。"
                        } else {
                            "从底部拍照按钮开始，录入你的第一件物品。"
                        },
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            } else {
                items(filteredItems, key = { it.item.id }) { itemDetail ->
                    ItemCard(
                        itemDetail = itemDetail,
                        onClick = { onNavigateToDetail(itemDetail.item.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroHeader(
    searchQuery: String,
    onOpenSearchCenter: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(OrangeStart, OrangeEnd, Color(0xFFFFB559))
                ),
                shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(252.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.24f), Color.Transparent),
                        radius = 620f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "有数",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "心中有数，遇事不怵",
                color = Color.White.copy(alpha = 0.94f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(18.dp))
            SearchBar(
                value = searchQuery,
                onValueChange = {},
                placeholder = "搜索物品、位置，或说点什么…",
                readOnly = true,
                onClick = onOpenSearchCenter
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "支持自然语言搜索，也可以从搜索中心查看建议与记录。",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OverviewRowCard(
    title: String,
    value: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    AppSurfaceCard(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

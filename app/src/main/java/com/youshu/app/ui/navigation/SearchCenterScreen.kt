package com.youshu.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
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
import com.youshu.app.ui.components.AppDecorativeBackground
import com.youshu.app.ui.components.AppSurfaceCard
import com.youshu.app.ui.components.EmptyState
import com.youshu.app.ui.components.ItemCard
import com.youshu.app.ui.components.SearchBar
import com.youshu.app.ui.components.SectionHeader
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.TagBlue
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.HomeViewModel

private val aiSearchHints = listOf(
    "冰箱里有什么快过期的",
    "我的充电器在哪",
    "还有多少牛奶",
    "厨房里的咖啡",
    "待评价的物品"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchCenterScreen(
    onBack: () -> Unit,
    onOpenLibrary: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val history by viewModel.searchHistory.collectAsState()
    val allItems by viewModel.allItems.collectAsState()

    val results = allItems.filter { itemDetail ->
        query.isNotBlank() && listOf(
            itemDetail.item.name,
            itemDetail.categoryName.orEmpty(),
            itemDetail.locationName.orEmpty(),
            itemDetail.item.note
        ).any { it.contains(query, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppDecorativeBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "搜索中心",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                SearchBar(
                    value = query,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = "例如：冰箱里有什么快过期的",
                    onSearch = {
                        if (query.isNotBlank()) {
                            viewModel.rememberSearch(query)
                        }
                    }
                )
            }

            if (query.isBlank()) {
                AppSurfaceCard(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    shadowElevation = 12.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(TagBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = OrangeStart,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(10.dp))
                        Column {
                            Text(
                                text = "AI 搜索建议",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "可以按位置、分类、效期或状态来搜索。",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(16.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aiSearchHints.forEach { hint ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White)
                                    .clickable {
                                        viewModel.updateSearchQuery(hint)
                                        viewModel.rememberSearch(hint)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = hint,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                if (history.isNotEmpty()) {
                    AppSurfaceCard(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        shadowElevation = 10.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = OrangeStart
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "搜索记录",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "清空",
                                fontSize = 12.sp,
                                color = TextHint,
                                modifier = Modifier.clickable { viewModel.clearSearchHistory() }
                            )
                        }

                        Spacer(modifier = Modifier.size(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            history.forEach { record ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0xFFF8F6FC))
                                        .clickable {
                                            viewModel.updateSearchQuery(record)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = record,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                AppSurfaceCard(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    shadowElevation = 10.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = OrangeStart
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "进入库房",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "查看全部物品、已用完和待评价状态。",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = "去查看",
                            fontSize = 13.sp,
                            color = OrangeStart,
                            modifier = Modifier.clickable(onClick = onOpenLibrary)
                        )
                    }
                }
            } else if (results.isEmpty()) {
                EmptyState(
                    title = "没有找到结果",
                    message = "可以试试换一个位置、分类、状态或更自然的描述方式。",
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "搜索结果",
                            subtitle = "共找到 ${results.size} 件匹配物品"
                        )
                    }
                    items(results, key = { it.item.id }) { itemDetail ->
                        ItemCard(
                            itemDetail = itemDetail,
                            onClick = { onNavigateToDetail(itemDetail.item.id) }
                        )
                    }
                }
            }
        }
    }
}

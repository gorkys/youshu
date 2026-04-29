package com.youshu.app.ui.screen.expiry

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.youshu.app.ui.components.PillTag
import com.youshu.app.ui.components.SectionHeader
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.StatusWarning
import com.youshu.app.ui.theme.TagOrange
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.viewmodel.ExpiryViewModel
import com.youshu.app.util.DateUtil

private data class ExpiryTab(val label: String, val days: Int?)

private val expiryTabs = listOf(
    ExpiryTab("全部", null),
    ExpiryTab("3天内", 3),
    ExpiryTab("7天内", 7),
    ExpiryTab("30天内", 30)
)

@Composable
fun ExpiryScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: ExpiryViewModel = hiltViewModel()
) {
    val expirableItems by viewModel.expirableItems.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val filteredItems = expirableItems
        .sortedBy { it.item.expireTime ?: Long.MAX_VALUE }
        .filter { itemDetail ->
            val expireTime = itemDetail.item.expireTime ?: return@filter false
            val days = DateUtil.daysUntil(expireTime)
            val threshold = expiryTabs[selectedTab].days
            threshold == null || days in 0..threshold.toLong()
        }

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
                Text(
                    text = "即将过期",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                expiryTabs.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    PillTag(
                        text = tab.label,
                        modifier = Modifier.clickable { selectedTab = index },
                        backgroundColor = if (selected) OrangeStart else Color.White,
                        contentColor = if (selected) Color.White else TextHint
                    )
                }
            }

            if (filteredItems.isEmpty()) {
                EmptyState(
                    title = "没有效期提醒",
                    message = "当前筛选条件下没有需要优先处理的物品。",
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 18.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "效期列表",
                            subtitle = "共 ${filteredItems.size} 件物品需要关注",
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    items(filteredItems, key = { it.item.id }) { itemDetail ->
                        ItemCard(
                            itemDetail = itemDetail,
                            onClick = { onNavigateToDetail(itemDetail.item.id) },
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            AppSurfaceCard(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                containerColor = TagOrange
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = StatusWarning
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = "小贴士",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "建议优先使用即将到期的物品，减少浪费。",
                            fontSize = 12.sp,
                            color = TextHint,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

package com.youshu.app.ui.screen.expiry

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.youshu.app.ui.components.EmptyState
import com.youshu.app.ui.components.ItemCard
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.StatusExpired
import com.youshu.app.ui.theme.StatusWarning
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.ExpiryViewModel

private data class ExpiryTab(val label: String, val days: Int)

private val expiryTabs = listOf(
    ExpiryTab("全部", 30),
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
    val allExpiringItems by viewModel.expiringItems.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    val filteredItems = when (selectedTab) {
        1 -> allExpiringItems.filter {
            val days = com.youshu.app.util.DateUtil.daysUntil(it.item.expireTime!!)
            days in 0..3
        }
        2 -> allExpiringItems.filter {
            val days = com.youshu.app.util.DateUtil.daysUntil(it.item.expireTime!!)
            days in 0..7
        }
        else -> allExpiringItems
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
                Text(
                    text = "即将过期",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Tab filter
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = OrangeStart,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = OrangeStart
                        )
                    }
                }
            ) {
                expiryTabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                tab.label,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = OrangeStart,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            if (filteredItems.isEmpty()) {
                EmptyState(
                    message = "没有即将过期的物品",
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Summary
                Text(
                    text = "${filteredItems.size}件物品即将过期",
                    fontSize = 13.sp,
                    color = StatusExpired,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.item.id }) { itemDetail ->
                        ItemCard(
                            itemDetail = itemDetail,
                            onClick = { onNavigateToDetail(itemDetail.item.id) }
                        )
                    }
                }
            }

            // Bottom tip banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = StatusWarning
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "建议优先使用即将过期的物品，减少浪费",
                        fontSize = 12.sp,
                        color = Color(0xFF8C6600)
                    )
                }
            }
        }
    }
}

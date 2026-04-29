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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.youshu.app.ui.components.EmptyState
import com.youshu.app.ui.components.ItemCard
import com.youshu.app.ui.components.SearchBar
import com.youshu.app.ui.theme.OrangeEnd
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.StatusExpired
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToExpiry: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val activeItems by viewModel.activeItems.collectAsState()
    val expiringCount by viewModel.expiringCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Top gradient header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(OrangeStart, OrangeEnd)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column {
                        Text(
                            text = "有数",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SearchBar(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier.clickable { onNavigateToSearch() }
                        )
                    }
                }
            }

            // Core cards - side by side
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick record card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToCamera() },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "快速记录物品",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "拍一下就记住",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(OrangeStart, OrangeEnd)
                                        )
                                    )
                                    .clickable { onNavigateToCamera() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "拍照",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Expiring items card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToExpiry() },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "即将过期",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${expiringCount}件物品",
                                fontSize = 12.sp,
                                color = if (expiringCount > 0) StatusExpired else TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(StatusExpired.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = StatusExpired,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Category chips
            if (categories.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            CategoryChip(text = category.name)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "最近物品",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "全部 ${activeItems.size}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Item list
            if (activeItems.isEmpty()) {
                item {
                    EmptyState(message = "还没有物品，拍照开始记录吧")
                }
            } else {
                items(activeItems, key = { it.item.id }) { itemDetail ->
                    ItemCard(
                        itemDetail = itemDetail,
                        onClick = { onNavigateToDetail(itemDetail.item.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable { /* navigate to category filter */ }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF1F1F1F)
        )
    }
}

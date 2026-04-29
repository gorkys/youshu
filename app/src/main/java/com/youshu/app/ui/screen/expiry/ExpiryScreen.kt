package com.youshu.app.ui.screen.expiry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.youshu.app.ui.components.EmptyState
import com.youshu.app.ui.components.ItemCard
import com.youshu.app.ui.theme.StatusExpired
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.ExpiryViewModel
import com.youshu.app.util.DateUtil

@Composable
fun ExpiryScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: ExpiryViewModel = hiltViewModel()
) {
    val expiringItems by viewModel.expiringItems.collectAsState()

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

            if (expiringItems.isEmpty()) {
                EmptyState(
                    message = "没有即将过期的物品",
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Summary
                Text(
                    text = "${expiringItems.size}件物品即将过期",
                    fontSize = 13.sp,
                    color = StatusExpired,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    items(expiringItems, key = { it.item.id }) { itemDetail ->
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

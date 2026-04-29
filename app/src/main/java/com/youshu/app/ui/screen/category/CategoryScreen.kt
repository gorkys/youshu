package com.youshu.app.ui.screen.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.data.local.entity.Location
import com.youshu.app.ui.components.EmptyState
import com.youshu.app.ui.components.ItemCard
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.CategoryViewModel

private val categoryIcons = mapOf(
    "食品" to "🍎", "药品" to "💊", "日用品" to "🧴", "电器" to "🔌",
    "衣物" to "👕", "书籍" to "📚", "工具" to "🔧", "其他" to "📦"
)

@Composable
fun CategoryScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val rootLocations by viewModel.rootLocations.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val selectedLocationId by viewModel.selectedLocationId.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val expandedLocations = remember { mutableStateMapOf<Long, Boolean>() }

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
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "分类",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Tab row
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
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        viewModel.selectCategory(null)
                    },
                    text = {
                        Text(
                            "按物品分类",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = OrangeStart,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        viewModel.selectLocation(null)
                    },
                    text = {
                        Text(
                            "按存放位置",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = OrangeStart,
                    unselectedContentColor = TextSecondary
                )
            }

            when (selectedTab) {
                0 -> CategoryListView(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onSelectCategory = { viewModel.selectCategory(it) },
                    filteredItems = filteredItems,
                    onNavigateToDetail = onNavigateToDetail
                )
                1 -> LocationTreeView(
                    rootLocations = rootLocations,
                    selectedLocationId = selectedLocationId,
                    expandedLocations = expandedLocations,
                    onSelectLocation = { viewModel.selectLocation(it) },
                    onToggleExpand = { id ->
                        expandedLocations[id] = !(expandedLocations[id] ?: false)
                    },
                    filteredItems = filteredItems,
                    onNavigateToDetail = onNavigateToDetail,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun CategoryListView(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelectCategory: (Long?) -> Unit,
    filteredItems: List<ItemDetail>,
    onNavigateToDetail: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Category list
        LazyColumn(
            modifier = Modifier.weight(0.4f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                CategoryListRow(
                    emoji = "📋",
                    name = "全部",
                    count = null,
                    isSelected = selectedCategoryId == null,
                    onClick = { onSelectCategory(null) }
                )
            }
            items(categories) { category ->
                CategoryListRow(
                    emoji = categoryIcons[category.name] ?: "📦",
                    name = category.name,
                    count = null,
                    isSelected = selectedCategoryId == category.id,
                    onClick = { onSelectCategory(category.id) }
                )
            }
        }

        // Filtered items
        if (selectedCategoryId != null) {
            HorizontalDivider(color = Color(0xFFF0F0F0))
            if (filteredItems.isEmpty()) {
                EmptyState(
                    message = "该分类下暂无物品",
                    modifier = Modifier.weight(0.6f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(0.6f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.item.id }) { itemDetail ->
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

@Composable
private fun CategoryListRow(
    emoji: String,
    name: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) OrangeStart.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with circular background
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF6F7FB)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = name,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) OrangeStart else Color(0xFF1F1F1F),
            modifier = Modifier.weight(1f)
        )

        if (count != null) {
            Text(
                text = "$count",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFFD0D0D0)
        )
    }
}

@Composable
private fun LocationTreeView(
    rootLocations: List<Location>,
    selectedLocationId: Long?,
    expandedLocations: Map<Long, Boolean>,
    onSelectLocation: (Long?) -> Unit,
    onToggleExpand: (Long) -> Unit,
    filteredItems: List<ItemDetail>,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: CategoryViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(0.4f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                LocationTreeRow(
                    icon = Icons.Default.Home,
                    name = "全部位置",
                    isExpanded = false,
                    hasChildren = false,
                    isSelected = selectedLocationId == null,
                    onClick = { onSelectLocation(null) }
                )
            }
            items(rootLocations) { location ->
                LocationNode(
                    location = location,
                    selectedLocationId = selectedLocationId,
                    expandedLocations = expandedLocations,
                    onSelectLocation = onSelectLocation,
                    onToggleExpand = onToggleExpand,
                    viewModel = viewModel,
                    depth = 0
                )
            }
        }

        if (selectedLocationId != null) {
            HorizontalDivider(color = Color(0xFFF0F0F0))
            if (filteredItems.isEmpty()) {
                EmptyState(
                    message = "该位置下暂无物品",
                    modifier = Modifier.weight(0.6f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(0.6f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.item.id }) { itemDetail ->
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

@Composable
private fun LocationNode(
    location: Location,
    selectedLocationId: Long?,
    expandedLocations: Map<Long, Boolean>,
    onSelectLocation: (Long?) -> Unit,
    onToggleExpand: (Long) -> Unit,
    viewModel: CategoryViewModel,
    depth: Int
) {
    val subLocations by viewModel.getSubLocations(location.id).collectAsState(initial = emptyList())
    val isExpanded = expandedLocations[location.id] ?: false
    val hasChildren = subLocations.isNotEmpty()

    Column {
        LocationTreeRow(
            icon = if (depth == 0) Icons.Default.Home else Icons.Default.LocationOn,
            name = location.name,
            isExpanded = isExpanded,
            hasChildren = hasChildren,
            isSelected = selectedLocationId == location.id,
            indent = depth,
            onClick = {
                onSelectLocation(location.id)
                if (hasChildren) onToggleExpand(location.id)
            }
        )

        AnimatedVisibility(
            visible = isExpanded && hasChildren,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                subLocations.forEach { subLocation ->
                    LocationNode(
                        location = subLocation,
                        selectedLocationId = selectedLocationId,
                        expandedLocations = expandedLocations,
                        onSelectLocation = onSelectLocation,
                        onToggleExpand = onToggleExpand,
                        viewModel = viewModel,
                        depth = depth + 1
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationTreeRow(
    icon: ImageVector,
    name: String,
    isExpanded: Boolean,
    hasChildren: Boolean,
    isSelected: Boolean,
    indent: Int = 0,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 24).dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) OrangeStart.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isSelected) OrangeStart else TextSecondary
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) OrangeStart else Color(0xFF1F1F1F),
            modifier = Modifier.weight(1f)
        )

        if (hasChildren) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TextSecondary
            )
        }
    }
}

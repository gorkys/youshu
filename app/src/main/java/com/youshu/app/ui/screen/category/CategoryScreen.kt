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
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.youshu.app.data.local.entity.Category
import com.youshu.app.data.local.entity.Location
import com.youshu.app.ui.components.AppDecorativeBackground
import com.youshu.app.ui.components.AppDialog
import com.youshu.app.ui.components.AppSurfaceCard
import com.youshu.app.ui.components.EmptyState
import com.youshu.app.ui.components.ItemCard
import com.youshu.app.ui.components.SectionHeader
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.StatusExpired
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.CategoryViewModel

private val categoryIcons = mapOf(
    "食品" to Icons.Default.Fastfood,
    "药品" to Icons.Default.MedicalServices,
    "日用品" to Icons.Default.Home,
    "数码" to Icons.Default.Devices,
    "衣物" to Icons.Default.Checkroom,
    "文具" to Icons.AutoMirrored.Filled.StickyNote2,
    "工具" to Icons.Default.Handyman,
    "其他" to Icons.Default.Apps
)

@Composable
fun CategoryScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val allLocations by viewModel.allLocations.collectAsState()
    val rootLocations by viewModel.rootLocations.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()
    val activeItems by viewModel.activeItems.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val selectedLocationId by viewModel.selectedLocationId.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val expandedLocations = remember { mutableStateMapOf<Long, Boolean>() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }

    val locationDescendants = remember(allLocations) { buildLocationDescendants(allLocations) }
    val categoryCounts = remember(categories, activeItems) {
        categories.associate { category ->
            category.id to activeItems.count { it.item.categoryId == category.id }
        }
    }
    val locationCounts = remember(allLocations, activeItems, locationDescendants) {
        allLocations.associate { location ->
            val ids = locationDescendants[location.id].orEmpty()
            location.id to activeItems.count { detail -> detail.item.locationId in ids }
        }
    }

    val selectedLocation = allLocations.firstOrNull { it.id == selectedLocationId }
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    val selectedLabel = when (selectedTab) {
        0 -> selectedCategory?.name ?: "全部分类"
        else -> selectedLocation?.name ?: "全部位置"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppDecorativeBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 12.dp, bottom = 102.dp)
        ) {
            Text(
                text = "分类",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = "按物品分类或按存放位置快速定位，也支持直接管理。",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            SegmentedTabs(
                selectedTab = selectedTab,
                onSelectTab = { index ->
                    selectedTab = index
                    if (index == 0) {
                        viewModel.selectCategory(null)
                    } else {
                        viewModel.selectLocation(null)
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedTab == 0) "管理分类" else "管理位置",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MiniActionButton(
                    icon = Icons.Default.Add,
                    contentDescription = "新增",
                    onClick = {
                        inputName = ""
                        showAddDialog = true
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                MiniActionButton(
                    icon = Icons.Default.DeleteOutline,
                    contentDescription = "删除",
                    enabled = if (selectedTab == 0) {
                        selectedCategoryId != null
                    } else {
                        selectedLocationId != null
                    },
                    tint = StatusExpired,
                    onClick = { showDeleteDialog = true }
                )
            }

            AppSurfaceCard(
                modifier = Modifier
                    .weight(0.4f)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shadowElevation = 12.dp
            ) {
                when (selectedTab) {
                    0 -> CategoryPanel(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        counts = categoryCounts,
                        onSelectCategory = viewModel::selectCategory
                    )

                    else -> LocationPanel(
                        rootLocations = rootLocations,
                        selectedLocationId = selectedLocationId,
                        expandedLocations = expandedLocations,
                        counts = locationCounts,
                        onSelectLocation = viewModel::selectLocation,
                        onToggleExpand = { id ->
                            expandedLocations[id] = !(expandedLocations[id] ?: false)
                        },
                        viewModel = viewModel
                    )
                }
            }

            AppSurfaceCard(
                modifier = Modifier
                    .weight(0.6f)
                    .padding(horizontal = 20.dp),
                shadowElevation = 12.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SectionHeader(
                        title = selectedLabel,
                        subtitle = "共 ${filteredItems.size} 件物品",
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    when {
                        (selectedTab == 0 && selectedCategoryId == null) || (selectedTab == 1 && selectedLocationId == null) -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyState(
                                    title = "先选择一个分组",
                                    message = if (selectedTab == 0) {
                                        "选择分类后，这里会展示该分类下的全部物品。"
                                    } else {
                                        "选择位置后，这里会展示该位置及子位置中的物品。"
                                    },
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                        }

                        filteredItems.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyState(
                                    title = "这里还是空的",
                                    message = "当前分组下还没有录入物品。",
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
        }
    }

    if (showAddDialog) {
        AppDialog(
            title = if (selectedTab == 0) "新增分类" else "新增位置",
            subtitle = if (selectedTab == 0) {
                "新增后会立刻出现在列表中。"
            } else {
                "如果当前已选中某个位置，则会新增到该位置下面。"
            },
            onDismissRequest = { showAddDialog = false },
            confirmText = "添加",
            confirmEnabled = inputName.isNotBlank(),
            onConfirm = {
                if (selectedTab == 0) {
                    viewModel.addCategory(inputName)
                } else {
                    viewModel.addLocation(inputName, selectedLocationId)
                }
                showAddDialog = false
            }
        ) {
            OutlinedTextField(
                value = inputName,
                onValueChange = { inputName = it },
                singleLine = true,
                label = {
                    Text(if (selectedTab == 0) "分类名称" else "位置名称")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDeleteDialog) {
        val deleteName = if (selectedTab == 0) selectedCategory?.name else selectedLocation?.name
        AppDialog(
            title = if (selectedTab == 0) "删除分类" else "删除位置",
            subtitle = "相关物品会保留，但关联关系可能会被清空。",
            onDismissRequest = { showDeleteDialog = false },
            confirmText = "删除",
            destructiveConfirm = true,
            onConfirm = {
                if (selectedTab == 0) {
                    viewModel.deleteCategory(selectedCategoryId)
                } else {
                    viewModel.deleteLocation(selectedLocationId)
                }
                showDeleteDialog = false
            }
        ) {
            Text(
                text = "确定要删除「${deleteName.orEmpty()}」吗？",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SegmentedTabs(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFF2EFF7))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SegmentItem(
            text = "按物品分类",
            selected = selectedTab == 0,
            modifier = Modifier.weight(1f),
            onClick = { onSelectTab(0) }
        )
        SegmentItem(
            text = "按存放位置",
            selected = selectedTab == 1,
            modifier = Modifier.weight(1f),
            onClick = { onSelectTab(1) }
        )
    }
}

@Composable
private fun SegmentItem(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) OrangeStart else TextHint
        )
    }
}

@Composable
private fun MiniActionButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    tint: Color = OrangeStart,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (enabled) Color.White else Color(0xFFF4F1F8))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else TextHint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CategoryPanel(
    categories: List<Category>,
    selectedCategoryId: Long?,
    counts: Map<Long, Int>,
    onSelectCategory: (Long?) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories, key = { it.id }) { category ->
            CategoryRow(
                category = category,
                count = counts[category.id] ?: 0,
                selected = selectedCategoryId == category.id,
                onClick = { onSelectCategory(category.id) }
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = categoryIcons[category.name] ?: Icons.Default.Apps
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) OrangeStart.copy(alpha = 0.1f) else Color(0xFFFAF8FE))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else Color(0xFFF1F5FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OrangeStart
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = category.name,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "${count}件",
            fontSize = 13.sp,
            color = if (selected) OrangeStart else TextSecondary
        )
    }
}

@Composable
private fun LocationPanel(
    rootLocations: List<Location>,
    selectedLocationId: Long?,
    expandedLocations: Map<Long, Boolean>,
    counts: Map<Long, Int>,
    onSelectLocation: (Long?) -> Unit,
    onToggleExpand: (Long) -> Unit,
    viewModel: CategoryViewModel
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(rootLocations, key = { it.id }) { location ->
            LocationNode(
                location = location,
                selectedLocationId = selectedLocationId,
                expandedLocations = expandedLocations,
                counts = counts,
                onSelectLocation = onSelectLocation,
                onToggleExpand = onToggleExpand,
                viewModel = viewModel,
                depth = 0
            )
        }
    }
}

@Composable
private fun LocationNode(
    location: Location,
    selectedLocationId: Long?,
    expandedLocations: Map<Long, Boolean>,
    counts: Map<Long, Int>,
    onSelectLocation: (Long?) -> Unit,
    onToggleExpand: (Long) -> Unit,
    viewModel: CategoryViewModel,
    depth: Int
) {
    val subLocations by viewModel.getSubLocations(location.id).collectAsState(initial = emptyList())
    val isExpanded = expandedLocations[location.id] ?: false
    val hasChildren = subLocations.isNotEmpty()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 16).dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (selectedLocationId == location.id) OrangeStart.copy(alpha = 0.1f)
                    else Color.Transparent
                )
                .clickable {
                    onSelectLocation(location.id)
                    if (hasChildren) onToggleExpand(location.id)
                }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (depth == 0) Icons.Default.Home else Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (selectedLocationId == location.id) OrangeStart else TextHint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = location.name,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "${counts[location.id] ?: 0}件",
                fontSize = 12.sp,
                color = TextSecondary
            )
            if (hasChildren) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextHint
                )
            }
        }

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
                        counts = counts,
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

private fun buildLocationDescendants(locations: List<Location>): Map<Long, Set<Long>> {
    val childrenMap = locations.groupBy { it.parentId }

    fun collectIds(locationId: Long): Set<Long> {
        val childIds = childrenMap[locationId].orEmpty().flatMap { collectIds(it.id) }
        return setOf(locationId) + childIds
    }

    return locations.associate { it.id to collectIds(it.id) }
}

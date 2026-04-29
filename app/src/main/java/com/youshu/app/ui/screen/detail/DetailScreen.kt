package com.youshu.app.ui.screen.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.youshu.app.data.local.entity.Item
import com.youshu.app.ui.components.AppDecorativeBackground
import com.youshu.app.ui.components.AppDialog
import com.youshu.app.ui.components.AppSurfaceCard
import com.youshu.app.ui.components.CategoryTag
import com.youshu.app.ui.components.GradientButton
import com.youshu.app.ui.components.PillTag
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.StatusExpired
import com.youshu.app.ui.theme.TagGreen
import com.youshu.app.ui.theme.TagGreenText
import com.youshu.app.ui.theme.TagOrange
import com.youshu.app.ui.theme.TagOrangeText
import com.youshu.app.ui.theme.TagRed
import com.youshu.app.ui.theme.TagRedText
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.ui.viewmodel.DetailViewModel
import com.youshu.app.util.DateUtil
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    itemId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showRateDialog by rememberSaveable { mutableStateOf(false) }
    var pendingRating by rememberSaveable { mutableIntStateOf(5) }
    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()
    var didJumpToInitialPage by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(items, itemId) {
        if (items.isEmpty()) {
            didJumpToInitialPage = false
            return@LaunchedEffect
        }
        val initialIndex = items.indexOfFirst { it.item.id == itemId }
        if (!didJumpToInitialPage && initialIndex >= 0) {
            pagerState.scrollToPage(initialIndex)
            didJumpToInitialPage = true
        } else if (pagerState.currentPage > items.lastIndex) {
            pagerState.scrollToPage(items.lastIndex)
        }
    }

    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "正在加载物品详情…",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
        return
    }

    val detail = items.getOrNull(pagerState.currentPage) ?: items.first()
    val item = detail.item

    Box(modifier = Modifier.fillMaxSize()) {
        AppDecorativeBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pageItem = items[page].item
                    if (pageItem.imagePath.isNotEmpty()) {
                        AsyncImage(
                            model = pageItem.imagePath,
                            contentDescription = pageItem.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(OrangeStart, Color(0xFFFFC266))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pageItem.name.take(1),
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.34f), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFFFFFBF6))
                            )
                        )
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.28f))
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }

                if (pagerState.currentPage > 0) {
                    ArrowCircleButton(
                        icon = Icons.Default.ChevronLeft,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                }

                if (pagerState.currentPage < items.lastIndex) {
                    ArrowCircleButton(
                        icon = Icons.Default.ChevronRight,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                }

                Text(
                    text = "${pagerState.currentPage + 1}/${items.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            AppSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-26).dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detail.categoryName?.let { CategoryTag(text = it) }
                    detail.locationName?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = it,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                DetailInfoRow("数量", "${item.quantity} ${item.unit}")
                item.expireTime?.let { expireTime ->
                    val days = DateUtil.daysUntil(expireTime)
                    val (container, content) = when {
                        days < 0 -> TagRed to TagRedText
                        days <= 7 -> TagRed to TagRedText
                        else -> TagOrange to TagOrangeText
                    }
                    DetailInfoRow(
                        label = "有效期",
                        value = DateUtil.formatDate(expireTime),
                        tail = {
                            PillTag(
                                text = DateUtil.expiryCountdownText(expireTime),
                                backgroundColor = container,
                                contentColor = content
                            )
                        }
                    )
                }
                item.price?.let { price ->
                    DetailInfoRow("价格", DateUtil.formatCurrency(price))
                }
                DetailInfoRow("添加时间", DateUtil.formatDateTime(item.createdAt))
                DetailInfoRow(
                    "状态",
                    when (item.status) {
                        Item.STATUS_USED_UP -> "已用完"
                        Item.STATUS_DISCARDED -> "已丢弃"
                        else -> "在用"
                    }
                )
                DetailInfoRow("备注", item.note.ifBlank { "无" })

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "评价记录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF8F6FC))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    when (item.rating) {
                        null -> Text(
                            text = if (item.status == Item.STATUS_USED_UP) {
                                "已用完，等待评价。你可以补充口感、效果或复购建议。"
                            } else {
                                "还没有评价，后续可用于记录口感、效果或复购建议。"
                            },
                            color = TextHint,
                            fontSize = 13.sp
                        )

                        else -> Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(5) { index ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (index < item.rating) OrangeStart else Color(0xFFE5DFEC),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = ratingText(item.rating),
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
                when (item.status) {
                    Item.STATUS_IN_USE -> {
                        GradientButton(
                            text = "已用完",
                            onClick = {
                                pendingRating = 5
                                showRateDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Item.STATUS_USED_UP -> {
                        GradientButton(
                            text = if (item.rating == null) "去评价" else "修改评价",
                            onClick = {
                                pendingRating = item.rating ?: 5
                                showRateDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionOutlineButton(
                        text = "编辑",
                        icon = Icons.Default.Edit,
                        onClick = { onEdit(item.id) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionOutlineButton(
                        text = "删除",
                        icon = Icons.Default.Delete,
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f),
                        destructive = true
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AppDialog(
            title = "确认删除",
            subtitle = "删除后不可恢复，相关图片也会一并移除。",
            onDismissRequest = { showDeleteDialog = false },
            confirmText = "删除",
            destructiveConfirm = true,
            onConfirm = {
                viewModel.delete(item)
                showDeleteDialog = false
                onBack()
            }
        ) {
            Text(
                text = "确定要删除「${item.name}」吗？",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }

    if (showRateDialog) {
        AppDialog(
            title = "给这件物品一个评价",
            subtitle = "5 星为最好，1 星代表终身拉黑。",
            onDismissRequest = { showRateDialog = false },
            confirmText = if (item.status == Item.STATUS_IN_USE) "标记并保存" else "保存评价",
            onConfirm = {
                if (item.status == Item.STATUS_IN_USE) {
                    viewModel.markAsUsed(item.id, pendingRating)
                } else {
                    viewModel.rateUsedItem(item.id, pendingRating)
                }
                showRateDialog = false
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { index ->
                    val selected = index < pendingRating
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .clickable { pendingRating = index + 1 }
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (selected) OrangeStart else Color(0xFFD5CFDE),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
            Text(
                text = ratingText(pendingRating),
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun ArrowCircleButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.22f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    tail: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        tail?.invoke()
    }
}

@Composable
private fun ActionOutlineButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false
) {
    val borderColor = if (destructive) StatusExpired.copy(alpha = 0.28f) else Color(0xFFEAE6F2)
    val contentColor = if (destructive) StatusExpired else TextSecondary

    Box(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

private fun ratingText(rating: Int): String {
    return when (rating) {
        5 -> "5 星，值得长期回购"
        4 -> "4 星，整体不错"
        3 -> "3 星，中规中矩"
        2 -> "2 星，谨慎再买"
        else -> "1 星，终身拉黑"
    }
}

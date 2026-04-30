package com.youshu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.youshu.app.data.local.entity.Item
import com.youshu.app.data.local.entity.ItemDetail
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.StatusNormal
import com.youshu.app.ui.theme.StatusWarning
import com.youshu.app.ui.theme.TagGreen
import com.youshu.app.ui.theme.TagGreenText
import com.youshu.app.ui.theme.TagOrange
import com.youshu.app.ui.theme.TagOrangeText
import com.youshu.app.ui.theme.TagRed
import com.youshu.app.ui.theme.TagRedText
import com.youshu.app.ui.theme.TextPrimary
import com.youshu.app.ui.theme.TextSecondary
import com.youshu.app.util.DateUtil

@Composable
fun ItemCard(
    itemDetail: ItemDetail,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = itemDetail.item

    AppSurfaceCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        shadowElevation = 12.dp,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.imagePath.isNotEmpty()) {
                AsyncImage(
                    model = item.imagePath,
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(OrangeStart.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.name.take(1),
                        color = OrangeStart,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    itemDetail.categoryName?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        CategoryTag(text = it)
                    }
                }

                itemDetail.locationName?.let { location ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = location,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "数量 ${item.quantity}${item.unit}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    item.price?.let { price ->
                        Text(
                            text = DateUtil.formatCurrency(price),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (item.status) {
                        Item.STATUS_USED_UP -> {
                            PillTag(
                                text = "已用完",
                                backgroundColor = TagOrange,
                                contentColor = TagOrangeText
                            )
                            PillTag(
                                text = item.rating?.let { "${it}星评价" } ?: "待评价",
                                backgroundColor = if (item.rating == null) TagRed else TagGreen,
                                contentColor = if (item.rating == null) TagRedText else TagGreenText
                            )
                        }

                        Item.STATUS_DISCARDED -> {
                            PillTag(
                                text = "已丢弃",
                                backgroundColor = TagRed,
                                contentColor = TagRedText
                            )
                        }

                        else -> {
                            item.expireTime?.let { expireTime ->
                                val days = DateUtil.daysUntil(expireTime)
                                val (container, content) = when {
                                    days < 0 -> TagRed to TagRedText
                                    days <= 3 -> TagRed to TagRedText
                                    days <= 7 -> StatusWarning.copy(alpha = 0.12f) to StatusWarning
                                    else -> StatusNormal.copy(alpha = 0.12f) to StatusNormal
                                }
                                PillTag(
                                    text = DateUtil.expiryCountdownText(expireTime),
                                    backgroundColor = container,
                                    contentColor = content
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFFF8F6FC), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondary
                )
            }
        }
    }
}

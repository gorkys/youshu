package com.youshu.app.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.youshu.app.ui.theme.OrangeStart
import com.youshu.app.ui.theme.TextHint
import com.youshu.app.ui.theme.TextPrimary

@Composable
fun ItemImageGallery(
    imagePaths: List<String>,
    modifier: Modifier = Modifier,
    onCapturePrimary: () -> Unit,
    onCaptureDetail: () -> Unit,
    onSelectNoImage: (() -> Unit)? = null,
    onRemoveImage: (Int) -> Unit,
    onMoveImage: (fromIndex: Int, toIndex: Int) -> Unit
) {
    if (imagePaths.isEmpty()) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionImageSlot(
                title = "拍主图",
                subtitle = "先拍一张封面图",
                icon = Icons.Default.PhotoCamera,
                onClick = onCapturePrimary,
                modifier = Modifier.weight(1f)
            )
            ActionImageSlot(
                title = "无图",
                subtitle = "直接填写主要信息",
                icon = Icons.Default.HideImage,
                onClick = { onSelectNoImage?.invoke() },
                modifier = Modifier.weight(1f)
            )
        }
        return
    }

    val density = LocalDensity.current
    val dragStepPx = with(density) { (120.dp + 10.dp).toPx() }
    var draggingPath by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragTargetIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val displayPaths = remember(imagePaths, draggingPath, dragTargetIndex) {
        val path = draggingPath
        val targetIndex = dragTargetIndex
        if (path == null || targetIndex !in imagePaths.indices) {
            imagePaths
        } else {
            imagePaths.toMutableList().apply {
                remove(path)
                add(targetIndex.coerceIn(0, size), path)
            }
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "长按图片可拖动调整顺序，第一张默认为主图",
            color = TextHint,
            fontSize = 12.sp
        )
        LazyRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(displayPaths, key = { _, path -> path }) { index, path ->
                val sourceIndex = imagePaths.indexOf(path).takeIf { it >= 0 } ?: index
                val isDragging = draggingPath == path
                val dragVisualOffset = if (isDragging) {
                    dragOffsetX - (dragTargetIndex - dragStartIndex) * dragStepPx
                } else {
                    0f
                }
                DraggableImageCard(
                    imagePath = path,
                    title = if (index == 0) "主图" else "细节图",
                    onRemove = { onRemoveImage(sourceIndex) },
                    onMove = { deltaX ->
                        dragOffsetX += deltaX
                        val startIndex = dragStartIndex.takeIf { it in imagePaths.indices } ?: sourceIndex
                        val targetIndex = (startIndex + calculateDragSteps(dragOffsetX, dragStepPx))
                            .coerceIn(0, imagePaths.lastIndex)
                        dragTargetIndex = targetIndex
                    },
                    onDragStart = {
                        draggingPath = path
                        dragStartIndex = sourceIndex
                        dragTargetIndex = sourceIndex
                        dragOffsetX = 0f
                    },
                    onDragEnd = {
                        val fromIndex = imagePaths.indexOf(path).takeIf { it >= 0 } ?: dragStartIndex
                        val toIndex = dragTargetIndex
                        if (fromIndex in imagePaths.indices && toIndex in imagePaths.indices && fromIndex != toIndex) {
                            onMoveImage(fromIndex, toIndex)
                        }
                        draggingPath = null
                        dragStartIndex = -1
                        dragTargetIndex = -1
                        dragOffsetX = 0f
                    },
                    onDragCancel = {
                        draggingPath = null
                        dragStartIndex = -1
                        dragTargetIndex = -1
                        dragOffsetX = 0f
                    },
                    isDragging = isDragging,
                    dragOffsetX = dragVisualOffset,
                    modifier = if (isDragging) {
                        Modifier
                    } else {
                        Modifier.animateItem(
                            placementSpec = spring(
                                dampingRatio = 0.84f,
                                stiffness = 360f
                            )
                        )
                    }
                )
            }
            item {
                ActionImageSlot(
                    title = "补拍细节",
                    subtitle = "继续添加图片",
                    icon = Icons.Default.Add,
                    onClick = onCaptureDetail,
                    modifier = Modifier.size(width = 120.dp, height = 120.dp)
                )
            }
        }
    }
}

@Composable
private fun DraggableImageCard(
    imagePath: String,
    title: String,
    onRemove: () -> Unit,
    onMove: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    isDragging: Boolean,
    dragOffsetX: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 120.dp, height = 120.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationX = dragOffsetX
                scaleX = if (isDragging) 1.03f else 1f
                scaleY = if (isDragging) 1.03f else 1f
                alpha = if (isDragging) 0.92f else 1f
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onMove(dragAmount.x)
                    }
                )
            }
    ) {
        ImagePreviewCard(
            imagePath = imagePath,
            title = title,
            onRemove = onRemove,
            modifier = Modifier.matchParentSize()
        )
    }
}

private fun calculateDragSteps(offsetPx: Float, stepPx: Float): Int {
    if (stepPx <= 0f || offsetPx == 0f) return 0
    val thresholdRatio = 0.68f
    val direction = if (offsetPx > 0f) 1 else -1
    val steps = kotlin.math.abs(offsetPx / stepPx)
    val wholeSteps = steps.toInt()
    val fractionalStep = steps - wholeSteps
    return direction * (wholeSteps + if (fractionalStep >= thresholdRatio) 1 else 0)
}

@Composable
private fun ImagePreviewCard(
    imagePath: String,
    title: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = title,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.12f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除图片",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActionImageSlot(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(164.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF8F6FC))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(OrangeStart.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangeStart,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextHint,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

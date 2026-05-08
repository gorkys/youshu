package com.youshu.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class SwipeActionSpec(
    val label: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val contentColor: Color = Color.White,
    val onClick: () -> Unit
)

@Composable
fun SwipeRevealItem(
    itemKey: Any,
    openedItemKey: Any?,
    onOpenedItemChange: (Any?) -> Unit,
    actions: List<SwipeActionSpec>,
    modifier: Modifier = Modifier,
    actionWidth: Dp = 92.dp,
    shape: Shape = RoundedCornerShape(22.dp),
    content: @Composable (closeActions: () -> Unit, isOpen: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val visibleActions = actions
    val revealWidthPx = visibleActions.size * actionWidthPx
    val offsetX = remember { Animatable(0f) }
    var contentHeightPx by remember { mutableIntStateOf(with(density) { 112.dp.roundToPx() }) }
    val maxSlidePx = revealWidthPx.coerceAtLeast(0f)
    val revealProgress = if (maxSlidePx == 0f) 0f else abs(offsetX.value / maxSlidePx).coerceIn(0f, 1f)
    val currentActionWidth = with(density) { max(actionWidthPx * revealProgress, 0f).toDp() }

    fun closeActions(updateOpenedKey: Boolean = true) {
        scope.launch {
            offsetX.animateTo(0f, tween(durationMillis = 180))
        }
        if (updateOpenedKey && openedItemKey == itemKey) {
            onOpenedItemChange(null)
        }
    }

    fun openActions() {
        onOpenedItemChange(itemKey)
        scope.launch {
            offsetX.animateTo(-maxSlidePx, tween(durationMillis = 180))
        }
    }

    LaunchedEffect(openedItemKey) {
        if (openedItemKey != itemKey && offsetX.value != 0f) {
            closeActions(updateOpenedKey = false)
        }
    }

    BoxWithConstraints(
        modifier = modifier.clipToBounds()
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(with(density) { contentHeightPx.toDp() }),
            horizontalArrangement = Arrangement.End
        ) {
            visibleActions.forEach { action ->
                SwipeActionButton(
                    action = action,
                    width = currentActionWidth,
                    height = with(density) { contentHeightPx.toDp() },
                    onClick = {
                        closeActions()
                        action.onClick()
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { contentHeightPx = it.height }
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(actions.size) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            val nextOffset = (offsetX.value + dragAmount).coerceIn(-maxSlidePx, 0f)
                            if (nextOffset != 0f && openedItemKey != itemKey) {
                                onOpenedItemChange(itemKey)
                            }
                            scope.launch { offsetX.snapTo(nextOffset) }
                        },
                        onDragEnd = {
                            if (abs(offsetX.value) > maxSlidePx * 0.35f) {
                                openActions()
                            } else {
                                closeActions()
                            }
                        }
                    )
                }
        ) {
            content(::closeActions, offsetX.value != 0f)
        }
    }
}

@Composable
private fun SwipeActionButton(
    action: SwipeActionSpec,
    width: Dp,
    height: Dp,
    fullWidth: Dp = 92.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(action.backgroundColor)
            .clipToBounds()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .requiredWidth(fullWidth)
                .padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = action.contentColor
            )
            Text(
                text = action.label,
                color = action.contentColor,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

package com.youshu.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youshu.app.ui.theme.TagBlue
import com.youshu.app.ui.theme.TagBlueText
import com.youshu.app.ui.theme.TagGreen
import com.youshu.app.ui.theme.TagGreenText
import com.youshu.app.ui.theme.TagOrange
import com.youshu.app.ui.theme.TagOrangeText
import com.youshu.app.ui.theme.TagPurple
import com.youshu.app.ui.theme.TagPurpleText

private val tagColorPairs = listOf(
    TagBlue to TagBlueText,
    TagGreen to TagGreenText,
    TagOrange to TagOrangeText,
    TagPurple to TagPurpleText
)

@Composable
fun CategoryTag(text: String, modifier: Modifier = Modifier) {
    val colorIndex = text.hashCode().mod(tagColorPairs.size).let { if (it < 0) it + tagColorPairs.size else it }
    val (bgColor, textColor) = tagColorPairs[colorIndex]

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = textColor
        )
    }
}

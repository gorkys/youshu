package com.youshu.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    val colorIndex = text.hashCode().mod(tagColorPairs.size).let {
        if (it < 0) it + tagColorPairs.size else it
    }
    val (bgColor, textColor) = tagColorPairs[colorIndex]
    PillTag(
        text = text,
        modifier = modifier,
        backgroundColor = bgColor,
        contentColor = textColor
    )
}

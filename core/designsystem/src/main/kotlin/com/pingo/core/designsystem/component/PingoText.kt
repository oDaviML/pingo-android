package com.pingo.core.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun PingoText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = PingoTheme.typography.body,
    color: Color = PingoTheme.colors.onBackground,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style
            .copy(color = color)
            .let { if (textAlign == null) it else it.copy(textAlign = textAlign) },
        overflow = overflow,
        maxLines = maxLines,
    )
}

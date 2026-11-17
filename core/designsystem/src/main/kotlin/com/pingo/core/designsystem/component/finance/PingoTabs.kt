package com.pingo.core.designsystem.component.finance

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoMotion
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.designsystem.theme.rememberReducedMotion

@Composable
fun PingoTabs(tabs: List<String>, selectedIndex: Int, onSelectTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (tabs.isEmpty()) return
    val safeIndex = selectedIndex.coerceIn(0, tabs.lastIndex)
    val reducedMotion = rememberReducedMotion()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tabWidth = maxWidth / tabs.size
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * safeIndex,
            animationSpec = if (reducedMotion) {
                snap()
            } else {
                tween(durationMillis = PingoMotion.SHORT_MS, easing = PingoMotion.StandardEasing)
            },
            label = "tab-indicator-offset",
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            TabRowContent(tabs = tabs, selectedIndex = safeIndex, onSelectTab = onSelectTab)
            TabIndicatorTrack(tabWidth = tabWidth, indicatorOffset = indicatorOffset)
        }
    }
}

@Composable
private fun TabRowContent(tabs: List<String>, selectedIndex: Int, onSelectTab: (Int) -> Unit) {
    val colors = PingoTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = PingoLayout.minTouchTarget)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelectTab(index) })
                    .padding(horizontal = PingoSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                PingoText(
                    text = label,
                    style = PingoTheme.typography.label,
                    color = if (selected) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TabIndicatorTrack(tabWidth: Dp, indicatorOffset: Dp) {
    val colors = PingoTheme.colors
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomStart)
                .background(color = colors.outlineVariant),
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .padding(horizontal = PingoSpacing.lg)
                .height(3.dp)
                .background(
                    color = colors.primary,
                    shape = RoundedCornerShape(topStart = PingoRadius.xs, topEnd = PingoRadius.xs),
                ),
        )
    }
}

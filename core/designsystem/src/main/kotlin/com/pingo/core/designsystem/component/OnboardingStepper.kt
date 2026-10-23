package com.pingo.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun OnboardingStepper(currentStep: Int, modifier: Modifier = Modifier, totalSteps: Int = 8, onSkip: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PingoSpacing.xl, end = PingoSpacing.xl, top = PingoSpacing.sm),
    ) {
        StepperHeader(currentStep = currentStep, totalSteps = totalSteps, onSkip = onSkip)
        StepperSegments(currentStep = currentStep, totalSteps = totalSteps)
    }
}

@Composable
private fun StepperHeader(currentStep: Int, totalSteps: Int, onSkip: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = "Passo $currentStep de $totalSteps",
            style = PingoTheme.typography.label,
            color = PingoTheme.colors.primary,
            modifier = Modifier.weight(1f),
        )

        if (onSkip != null) {
            PingoText(
                text = "Pular",
                style = PingoTheme.typography.label,
                color = PingoTheme.colors.onSurfaceVariant,
                modifier = Modifier.clickable(role = Role.Button, onClick = onSkip),
            )
        }
    }
}

@Composable
private fun StepperSegments(currentStep: Int, totalSteps: Int) {
    val filledColor = PingoTheme.colors.primary
    val emptyColor = PingoTheme.colors.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PingoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(totalSteps) { index ->
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(PingoRadius.full)),
            ) {
                drawRect(color = if (index < currentStep) filledColor else emptyColor)
            }
        }
    }
}

package com.pingo.feature.onboarding.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
internal fun PassosNumerados(passos: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.md)) {
        passos.forEachIndexed { indice, passo ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                NumeroDoPasso(numero = indice + 1)

                PingoText(
                    text = passo,
                    style = PingoTheme.typography.body,
                    color = PingoTheme.colors.onBackground,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NumeroDoPasso(numero: Int) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color = PingoTheme.colors.primary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PingoText(
            text = numero.toString(),
            style = PingoTheme.typography.labelSmall,
            color = PingoTheme.colors.onPrimary,
        )
    }
}

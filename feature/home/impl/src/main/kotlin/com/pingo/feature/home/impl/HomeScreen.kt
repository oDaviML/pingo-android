package com.pingo.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoSpacing

@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PingoSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        PingoText(text = "Início")

        PingoButton(
            text = "Configurações",
            onClick = onOpenSettings,
            type = PingoButtonType.Text,
        )
    }
}

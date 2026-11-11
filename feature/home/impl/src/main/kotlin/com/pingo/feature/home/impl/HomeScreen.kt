package com.pingo.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun HomeScreen(onOpenSettings: () -> Unit, onOpenCaptura: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.atualizarCaptura()
    }

    HomeContent(
        state = state,
        onOpenSettings = onOpenSettings,
        onOpenCaptura = onOpenCaptura,
    )
}

@Composable
fun HomeContent(state: HomeUiState, onOpenSettings: () -> Unit, onOpenCaptura: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PingoSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        PingoText(text = "Início")

        if (!state.capturaAutomatica) {
            AvisoDeCapturaParada(onCorrigir = onOpenCaptura)
        }

        PingoButton(
            text = "Configurações",
            onClick = onOpenSettings,
            type = PingoButtonType.Text,
        )
    }
}

@Composable
private fun AvisoDeCapturaParada(onCorrigir: () -> Unit) {
    PingoCard(contentPadding = PingoSpacing.lg, gap = PingoSpacing.md) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PingoIcon(
                imageVector = PingoIcons.TriangleAlert,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = PingoTheme.colors.warning,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                PingoText(
                    text = "Captura parada",
                    style = PingoTheme.typography.bodyStrong,
                    color = PingoTheme.colors.warning,
                )
                PingoText(
                    text = "Sem acesso às notificações, o Pingo está no modo manual.",
                    style = PingoTheme.typography.bodySmall,
                    color = PingoTheme.colors.onSurfaceVariant,
                )
            }
        }
        PingoButton(
            text = "Corrigir",
            onClick = onCorrigir,
            type = PingoButtonType.Tonal,
            size = PingoButtonSize.Small,
        )
    }
}

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
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.component.PingoBottomBar
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
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenCaptura: () -> Unit,
    onOpenNovo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.atualizarCaptura()
    }

    HomeContent(
        state = state,
        onOpenSettings = onOpenSettings,
        onOpenCaptura = onOpenCaptura,
        onOpenNovo = onOpenNovo,
        onSelecionarAba = onSelecionarAba,
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onOpenSettings: () -> Unit,
    onOpenCaptura: () -> Unit,
    onOpenNovo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(PingoSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        ) {
            PingoText(text = "Início")

            if (state.capturaParada) {
                AvisoDeCapturaParada(descricao = descricaoDaCaptura(state), onCorrigir = onOpenCaptura)
            }

            PingoButton(
                text = "Configurações",
                onClick = onOpenSettings,
                type = PingoButtonType.Text,
            )
        }

        PingoBottomBar(
            selecionada = AbaInferior.INICIO,
            onSelecionar = onSelecionarAba,
            onNovo = onOpenNovo,
        )
    }
}

@Composable
private fun AvisoDeCapturaParada(descricao: String, onCorrigir: () -> Unit) {
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
                    text = descricao,
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

private fun descricaoDaCaptura(state: HomeUiState): String = if (!state.capturaAutomatica) {
    "Sem acesso às notificações, o Pingo está no modo manual."
} else {
    "Nenhuma notificação lida há 2 dias. O Pingo pode não estar vendo seus bancos."
}

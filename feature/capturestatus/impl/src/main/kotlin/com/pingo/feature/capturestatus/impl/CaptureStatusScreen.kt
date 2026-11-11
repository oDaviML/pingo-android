package com.pingo.feature.capturestatus.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.PingoTopBar
import com.pingo.core.designsystem.component.SectionHeader
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.notifications.IntentsDeSistema
import java.time.ZoneId

@Composable
fun CaptureStatusScreen(onBack: () -> Unit, viewModel: CaptureStatusViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.atualizarEstados()
    }

    CaptureStatusContent(
        state = state,
        onBack = onBack,
        onReconectar = viewModel::reconectar,
        onAtualizarRegras = viewModel::atualizarRegras,
        onAbrirAjuste = { item -> IntentsDeSistema.abrir(context, intencaoPara(item, context.packageName)) },
    )
}

@Composable
fun CaptureStatusContent(
    state: CaptureStatusUiState,
    onBack: () -> Unit,
    onReconectar: () -> Unit,
    onAtualizarRegras: () -> Unit,
    onAbrirAjuste: (ItemDePermissao) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zona = ZoneId.systemDefault()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        PingoTopBar(title = "Status da captura", onBack = onBack)

        ConteudoDaCaptura(
            state = state,
            zona = zona,
            onReconectar = onReconectar,
            onAtualizarRegras = onAtualizarRegras,
            onAbrirAjuste = onAbrirAjuste,
        )
    }
}

@Composable
private fun ConteudoDaCaptura(
    state: CaptureStatusUiState,
    zona: ZoneId,
    onReconectar: () -> Unit,
    onAtualizarRegras: () -> Unit,
    onAbrirAjuste: (ItemDePermissao) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        StatusDaCapturaCard(state = state, zona = zona)

        SectionHeader("Neste mês")
        ContadoresDoMesGrid(contadores = state.contadores)

        SectionHeader("Última notificação por app")
        LeiturasPorAppCard(leituras = state.leiturasPorApp, agora = state.agora, zona = zona)

        SectionHeader("Permissões")
        state.permissoes.forEach { permissao ->
            PermissaoDaCapturaCard(permissao = permissao, onCorrigir = { onAbrirAjuste(permissao.item) })
        }

        RegrasDoParserCard(state = state, zona = zona, onAtualizarRegras = onAtualizarRegras)

        AcoesDaCaptura(state = state, onReconectar = onReconectar, onAbrirAjuste = onAbrirAjuste)
    }
}

@Composable
private fun AcoesDaCaptura(state: CaptureStatusUiState, onReconectar: () -> Unit, onAbrirAjuste: (ItemDePermissao) -> Unit) {
    state.mensagem?.let { mensagem ->
        PingoText(
            text = mensagem,
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }

    state.ajustePrioritario?.let { item ->
        PingoButton(
            text = "Corrigir permissão",
            onClick = { onAbrirAjuste(item) },
            type = PingoButtonType.Primary,
            fillWidth = true,
        )
    }

    PingoButton(
        text = "Reconectar",
        onClick = onReconectar,
        type = PingoButtonType.Outline,
        fillWidth = true,
        loading = state.reconectando,
    )

    Spacer(modifier = Modifier.height(PingoSpacing.lg))
}

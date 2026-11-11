package com.pingo.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.component.PingoBottomBar
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import java.time.ZoneId

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenCaptura: () -> Unit,
    onOpenRevisao: () -> Unit,
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
        onOpenRevisao = onOpenRevisao,
        onOpenNovo = onOpenNovo,
        onMesAnterior = viewModel::mesAnterior,
        onMesProximo = viewModel::mesProximo,
        onSelecionarAba = onSelecionarAba,
    )
}

@Composable
@Suppress("LongParameterList")
fun HomeContent(
    state: HomeUiState,
    onOpenSettings: () -> Unit,
    onOpenCaptura: () -> Unit,
    onOpenRevisao: () -> Unit,
    onOpenNovo: () -> Unit,
    onMesAnterior: () -> Unit,
    onMesProximo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit = {},
    zona: ZoneId = ZoneId.systemDefault(),
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PingoLayout.screenHorizontal, vertical = PingoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        ) {
            CabecalhoDaInicio(onOpenSettings)
            SeletorDeMes(state = state, onMesAnterior = onMesAnterior, onMesProximo = onMesProximo)

            if (state.capturaParada) {
                AvisoDeCapturaParada(descricao = descricaoDaCaptura(state), onCorrigir = onOpenCaptura)
            }

            if (state.pendentes > 0) {
                AvisoDeRevisao(quantidade = state.pendentes, onAbrir = onOpenRevisao)
            }

            when {
                state.carregando -> Unit
                state.vazia -> InicioVazio(onOpenNovo = onOpenNovo)
                else -> {
                    SaldoDoMes(state)
                    TopCategorias(state)
                    UltimosLancamentos(
                        state = state,
                        onVerTudo = { onSelecionarAba(AbaInferior.LANCAMENTOS) },
                        zona = zona,
                    )
                }
            }
        }

        PingoBottomBar(
            selecionada = AbaInferior.INICIO,
            onSelecionar = onSelecionarAba,
            onNovo = onOpenNovo,
        )
    }
}

@Composable
private fun CabecalhoDaInicio(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = PingoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = "Início",
            style = PingoTheme.typography.headline,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        PingoButton(
            text = "Configurações",
            onClick = onOpenSettings,
            type = PingoButtonType.Text,
            size = PingoButtonSize.Small,
        )
    }
}

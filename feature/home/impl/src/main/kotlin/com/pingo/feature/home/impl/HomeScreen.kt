package com.pingo.feature.home.impl

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.component.PingoBottomBar
import com.pingo.core.designsystem.component.PingoHomeTopBar
import com.pingo.core.designsystem.component.PingoSkeletonList
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.OrigemLancamento
import java.time.ZoneId

@Suppress("LongParameterList")
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenCaptura: () -> Unit,
    onOpenRevisao: () -> Unit,
    onOpenNovo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    mostrarBarraInferior: Boolean = true,
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
        mostrarBarraInferior = mostrarBarraInferior,
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
    mostrarBarraInferior: Boolean = true,
) {
    val animarTopoId = rememberNovoCapturadoNoTopo(state = state)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        CorpoDaInicio(
            state = state,
            onOpenSettings = onOpenSettings,
            onOpenCaptura = onOpenCaptura,
            onOpenRevisao = onOpenRevisao,
            onOpenNovo = onOpenNovo,
            onMesAnterior = onMesAnterior,
            onMesProximo = onMesProximo,
            onSelecionarAba = onSelecionarAba,
            zona = zona,
            animarTopoId = animarTopoId.value,
            onAnimacaoConcluida = { animarTopoId.value = null },
            modifier = Modifier.weight(1f),
        )

        if (mostrarBarraInferior) {
            PingoBottomBar(
                selecionada = AbaInferior.INICIO,
                onSelecionar = onSelecionarAba,
                onNovo = onOpenNovo,
            )
        }
    }
}

@Composable
private fun rememberNovoCapturadoNoTopo(state: HomeUiState): androidx.compose.runtime.MutableState<String?> {
    var inicializado by remember { mutableStateOf(false) }
    var ultimoTopoId by remember { mutableStateOf<String?>(null) }
    val animarTopoId = remember { mutableStateOf<String?>(null) }
    val topo = state.ultimos.firstOrNull()?.lancamento

    LaunchedEffect(state.carregando, topo?.publicId) {
        if (state.carregando) {
            return@LaunchedEffect
        }
        val novoId = topo?.publicId
        if (!inicializado) {
            ultimoTopoId = novoId
            inicializado = true
            return@LaunchedEffect
        }
        if (novoId != null && novoId != ultimoTopoId) {
            ultimoTopoId = novoId
            if (topo.origem == OrigemLancamento.NOTIFICACAO) {
                animarTopoId.value = novoId
            }
        }
    }
    return animarTopoId
}

@Suppress("LongParameterList")
@Composable
private fun CorpoDaInicio(
    state: HomeUiState,
    onOpenSettings: () -> Unit,
    onOpenCaptura: () -> Unit,
    onOpenRevisao: () -> Unit,
    onOpenNovo: () -> Unit,
    onMesAnterior: () -> Unit,
    onMesProximo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit,
    zona: ZoneId,
    animarTopoId: String?,
    onAnimacaoConcluida: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        PingoHomeTopBar(
            greeting = state.saudacao,
            name = "Pingo",
            onNotificationsClick = {
                if (state.pendentes > 0) onOpenRevisao() else onOpenCaptura()
            },
            onSettingsClick = onOpenSettings,
            hasUnreadNotifications = state.pendentes > 0 || state.capturaParada,
        )

        SecoesDaInicio(
            state = state,
            onOpenCaptura = onOpenCaptura,
            onOpenRevisao = onOpenRevisao,
            onOpenNovo = onOpenNovo,
            onMesAnterior = onMesAnterior,
            onMesProximo = onMesProximo,
            onSelecionarAba = onSelecionarAba,
            zona = zona,
            animarTopoId = animarTopoId,
            onAnimacaoConcluida = onAnimacaoConcluida,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun SecoesDaInicio(
    state: HomeUiState,
    onOpenCaptura: () -> Unit,
    onOpenRevisao: () -> Unit,
    onOpenNovo: () -> Unit,
    onMesAnterior: () -> Unit,
    onMesProximo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit,
    zona: ZoneId,
    animarTopoId: String?,
    onAnimacaoConcluida: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PingoSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        SeletorDeMes(state = state, onMesAnterior = onMesAnterior, onMesProximo = onMesProximo)

        if (state.capturaParada) {
            AvisoDeCapturaParada(descricao = descricaoDaCaptura(state), onCorrigir = onOpenCaptura)
        }

        if (state.pendentes > 0) {
            AvisoDeRevisao(quantidade = state.pendentes, onAbrir = onOpenRevisao)
        }

        when {
            state.carregando -> PingoSkeletonList(count = 4)
            state.vazia -> InicioVazio(onOpenNovo = onOpenNovo)
            else -> {
                SaldoDoMes(state)
                TopCategorias(
                    state = state,
                    onVerRelatorio = { onSelecionarAba(AbaInferior.RELATORIOS) },
                )
                UltimosLancamentos(
                    state = state,
                    onVerTudo = { onSelecionarAba(AbaInferior.LANCAMENTOS) },
                    zona = zona,
                    animarTopoId = animarTopoId,
                    onAnimacaoConcluida = onAnimacaoConcluida,
                )
            }
        }
        Spacer(modifier = Modifier.height(PingoSpacing.lg))
    }
}

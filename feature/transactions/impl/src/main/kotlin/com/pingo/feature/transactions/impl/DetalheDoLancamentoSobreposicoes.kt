package com.pingo.feature.transactions.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.pingo.core.designsystem.component.PingoDestructiveDialog
import java.time.ZoneId

@Composable
internal fun SobreposicoesDoDetalhe(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit, zona: ZoneId) {
    if (state.calendarioVisivel) {
        CalendarioDoLancamento(
            data = state.dataHora.atZone(zona).toLocalDate(),
            hoje = state.hoje,
            onSelecionar = { onEvent(DetalheDoLancamentoEvent.SelecionarData(it)) },
            onFechar = { onEvent(DetalheDoLancamentoEvent.FecharCalendario) },
        )
    }

    if (state.dialogoDeExclusao) {
        PingoDestructiveDialog(
            title = "Excluir lançamento?",
            description = descricaoDaExclusao(state),
            confirmText = "Excluir",
            confirmLoading = state.excluindo,
            onConfirm = { onEvent(DetalheDoLancamentoEvent.ConfirmarExclusao) },
            onDismissRequest = { onEvent(DetalheDoLancamentoEvent.CancelarExclusao) },
            modifier = Modifier.testTag("dialogo-exclusao"),
        )
    }

    if (state.dialogoDeExclusaoParcelas) {
        PingoDestructiveDialog(
            title = "Excluir todas as parcelas?",
            description = descricaoExclusaoTodasParcelas(state),
            confirmText = "Excluir todas",
            confirmLoading = state.excluindo,
            onConfirm = { onEvent(DetalheDoLancamentoEvent.ConfirmarExclusaoParcelas) },
            onDismissRequest = { onEvent(DetalheDoLancamentoEvent.CancelarExclusaoParcelas) },
            modifier = Modifier.testTag("dialogo-exclusao-parcelas"),
        )
    }
}

@Composable
internal fun PainelAtualDoDetalhe(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit) {
    val painel = state.painel ?: return
    if (painel == PainelDoDetalhe.PARCELAS) {
        PainelDeParcelasSheet(
            totalParcelas = state.totalParcelas,
            modoParcelamento = state.modoParcelamento,
            resumo = RotulosDoDetalhe.resumoParcelas(state),
            onSelecionarParcelas = { total -> onEvent(DetalheDoLancamentoEvent.SelecionarParcelas(total)) },
            onSelecionarModo = { modo -> onEvent(DetalheDoLancamentoEvent.SelecionarModoParcelamento(modo)) },
            onFechar = { onEvent(DetalheDoLancamentoEvent.FecharPainel) },
        )
        return
    }
    val config = configuracaoDoPainel(state, painel)
    PainelDeSelecao(
        titulo = config.titulo,
        opcoes = opcoesDoPainel(state, painel),
        selecionado = config.selecionado,
        prefixo = config.prefixo,
        testTag = "painel-detalhe",
        onSelecionar = { publicId -> onEvent(eventoDeSelecao(painel, publicId)) },
        onFechar = { onEvent(DetalheDoLancamentoEvent.FecharPainel) },
    )
}

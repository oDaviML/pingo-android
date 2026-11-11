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
}

@Composable
internal fun PainelAtualDoDetalhe(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit) {
    val painel = state.painel ?: return
    val selecionado = when (painel) {
        PainelDoDetalhe.INSTRUMENTO -> state.instrumento?.publicId
        PainelDoDetalhe.DESTINO -> state.destino?.publicId
        PainelDoDetalhe.MARCAR_TRANSFERENCIA, PainelDoDetalhe.CATEGORIA -> null
    }
    val titulo = when (painel) {
        PainelDoDetalhe.INSTRUMENTO -> if (state.ehTransferencia) "Conta de origem" else "Instrumento"
        PainelDoDetalhe.DESTINO -> "Conta de destino"
        PainelDoDetalhe.MARCAR_TRANSFERENCIA -> "Escolha a conta de destino"
        PainelDoDetalhe.CATEGORIA -> "Categoria"
    }
    val prefixo = when (painel) {
        PainelDoDetalhe.INSTRUMENTO -> "instrumento-detalhe"
        PainelDoDetalhe.DESTINO -> "destino-detalhe"
        PainelDoDetalhe.MARCAR_TRANSFERENCIA -> "destino-marcacao"
        PainelDoDetalhe.CATEGORIA -> "categoria-detalhe"
    }

    PainelDeSelecao(
        titulo = titulo,
        opcoes = opcoesDoPainel(state, painel),
        selecionado = selecionado,
        prefixo = prefixo,
        testTag = "painel-detalhe",
        onSelecionar = { publicId -> onEvent(eventoDeSelecao(painel, publicId)) },
        onFechar = { onEvent(DetalheDoLancamentoEvent.FecharPainel) },
    )
}

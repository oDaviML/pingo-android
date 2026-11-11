package com.pingo.feature.transactions.impl

import androidx.compose.runtime.Composable

@Composable
internal fun PainelDoNovoLancamento(state: NovoLancamentoUiState, painel: PainelDoNovoLancamento, onEvent: (NovoLancamentoEvent) -> Unit) {
    val opcoes = when (painel) {
        PainelDoNovoLancamento.CATEGORIA -> state.categorias.map { OpcaoDePainel(it.publicId, it.nome) }
        PainelDoNovoLancamento.INSTRUMENTO, PainelDoNovoLancamento.DESTINO -> state.instrumentos.map {
            OpcaoDePainel(it.publicId, it.rotulo)
        }
    }
    val selecionado = when (painel) {
        PainelDoNovoLancamento.CATEGORIA -> state.categoria?.publicId
        PainelDoNovoLancamento.DESTINO -> state.destino?.publicId
        PainelDoNovoLancamento.INSTRUMENTO -> state.instrumento?.publicId
    }

    PainelDeSelecao(
        titulo = tituloDoPainel(state, painel),
        opcoes = opcoes,
        selecionado = selecionado,
        prefixo = if (painel == PainelDoNovoLancamento.CATEGORIA) "categoria-opcao" else "instrumento-opcao",
        testTag = "painel-novo-lancamento",
        onSelecionar = { publicId -> onEvent(eventoDeSelecao(painel, publicId)) },
        onFechar = { onEvent(NovoLancamentoEvent.FecharPainel) },
    )
}

private fun tituloDoPainel(state: NovoLancamentoUiState, painel: PainelDoNovoLancamento): String = when (painel) {
    PainelDoNovoLancamento.CATEGORIA -> "Categoria"
    PainelDoNovoLancamento.DESTINO -> "Conta de destino"
    PainelDoNovoLancamento.INSTRUMENTO ->
        if (state.tipo == TipoDoLancamentoManual.TRANSFERENCIA) "Conta de origem" else "Instrumento"
}

private fun eventoDeSelecao(painel: PainelDoNovoLancamento, publicId: String): NovoLancamentoEvent =
    if (painel == PainelDoNovoLancamento.CATEGORIA) {
        NovoLancamentoEvent.SelecionarCategoria(publicId)
    } else {
        NovoLancamentoEvent.SelecionarInstrumento(publicId)
    }

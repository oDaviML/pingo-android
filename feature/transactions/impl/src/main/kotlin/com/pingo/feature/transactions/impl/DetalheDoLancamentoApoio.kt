package com.pingo.feature.transactions.impl

import com.pingo.core.ui.Valores

internal fun opcoesDoPainel(state: DetalheDoLancamentoUiState, painel: PainelDoDetalhe): List<OpcaoDePainel> = when (painel) {
    PainelDoDetalhe.INSTRUMENTO -> state.instrumentos.map { OpcaoDePainel(it.publicId, it.rotulo) }
    PainelDoDetalhe.DESTINO, PainelDoDetalhe.MARCAR_TRANSFERENCIA -> state.destinosDaTransferencia.map {
        OpcaoDePainel(it.publicId, it.rotulo)
    }

    PainelDoDetalhe.CATEGORIA -> state.categorias.map { OpcaoDePainel(it.publicId, it.nome) }
}

internal fun eventoDeSelecao(painel: PainelDoDetalhe, publicId: String): DetalheDoLancamentoEvent = when (painel) {
    PainelDoDetalhe.INSTRUMENTO -> DetalheDoLancamentoEvent.SelecionarInstrumento(publicId)
    PainelDoDetalhe.DESTINO, PainelDoDetalhe.MARCAR_TRANSFERENCIA -> DetalheDoLancamentoEvent.SelecionarDestino(publicId)
    PainelDoDetalhe.CATEGORIA -> DetalheDoLancamentoEvent.SelecionarCategoria(publicId)
}

internal fun descricaoDaExclusao(state: DetalheDoLancamentoUiState): String {
    val valor = Valores.formatar(state.valorCentavos)
    val estabelecimento = state.estabelecimento.takeIf { it.isNotBlank() }
    val alvo = estabelecimento?.let { "$it, $valor" } ?: valor

    return "$alvo. O lançamento sai da lista e não entra mais nos totais."
}

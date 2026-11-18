package com.pingo.feature.transactions.impl

import com.pingo.core.ui.Valores

internal fun configuracaoDoPainel(state: DetalheDoLancamentoUiState, painel: PainelDoDetalhe): ConfiguracaoPainelDetalhe = when (painel) {
    PainelDoDetalhe.INSTRUMENTO -> ConfiguracaoPainelDetalhe(
        selecionado = state.instrumento?.publicId,
        titulo = if (state.ehTransferencia) "Conta de origem" else "Instrumento",
        prefixo = "instrumento-detalhe",
    )

    PainelDoDetalhe.DESTINO -> ConfiguracaoPainelDetalhe(
        selecionado = state.destino?.publicId,
        titulo = "Conta de destino",
        prefixo = "destino-detalhe",
    )

    PainelDoDetalhe.MARCAR_TRANSFERENCIA -> ConfiguracaoPainelDetalhe(
        selecionado = null,
        titulo = "Escolha a conta de destino",
        prefixo = "destino-marcacao",
    )

    PainelDoDetalhe.CATEGORIA -> ConfiguracaoPainelDetalhe(
        selecionado = null,
        titulo = "Categoria",
        prefixo = "categoria-detalhe",
    )

    PainelDoDetalhe.PARCELAS -> ConfiguracaoPainelDetalhe(
        selecionado = null,
        titulo = "Parcelamento",
        prefixo = "parcelas-detalhe",
    )
}

internal fun opcoesDoPainel(state: DetalheDoLancamentoUiState, painel: PainelDoDetalhe): List<OpcaoDePainel> = when (painel) {
    PainelDoDetalhe.INSTRUMENTO -> state.instrumentos.map { OpcaoDePainel(it.publicId, it.rotulo) }
    PainelDoDetalhe.DESTINO, PainelDoDetalhe.MARCAR_TRANSFERENCIA -> state.destinosDaTransferencia.map {
        OpcaoDePainel(it.publicId, it.rotulo)
    }

    PainelDoDetalhe.CATEGORIA, PainelDoDetalhe.PARCELAS -> state.categorias.map { OpcaoDePainel(it.publicId, it.nome) }
}

internal fun eventoDeSelecao(painel: PainelDoDetalhe, publicId: String): DetalheDoLancamentoEvent = when (painel) {
    PainelDoDetalhe.INSTRUMENTO -> DetalheDoLancamentoEvent.SelecionarInstrumento(publicId)
    PainelDoDetalhe.DESTINO, PainelDoDetalhe.MARCAR_TRANSFERENCIA -> DetalheDoLancamentoEvent.SelecionarDestino(publicId)
    PainelDoDetalhe.CATEGORIA, PainelDoDetalhe.PARCELAS -> DetalheDoLancamentoEvent.SelecionarCategoria(publicId)
}

internal fun descricaoDaExclusao(state: DetalheDoLancamentoUiState): String {
    val valor = Valores.formatar(state.valorCentavos)
    val estabelecimento = state.estabelecimento.takeIf { it.isNotBlank() }
    val alvo = estabelecimento?.let { "$it, $valor" } ?: valor
    val parcelamento = state.parcelamento

    return if (parcelamento != null) {
        "$alvo (parcela ${parcelamento.parcelaNumero} de ${parcelamento.totalParcelas}). Apenas esta parcela será excluída."
    } else {
        "$alvo. O lançamento sai da lista e não entra mais nos totais."
    }
}

internal fun descricaoExclusaoTodasParcelas(state: DetalheDoLancamentoUiState): String {
    val parcelamento = state.parcelamento ?: return descricaoDaExclusao(state)
    val estabelecimento = state.estabelecimento.takeIf { it.isNotBlank() } ?: "Esta compra parcelada"
    return "$estabelecimento em ${parcelamento.totalParcelas}x. Todas as parcelas serão excluídas das faturas."
}

package com.pingo.feature.transactions.impl

import java.time.LocalDate

sealed interface DetalheDoLancamentoEvent {
    data object AlternarTeclado : DetalheDoLancamentoEvent

    data class DigitoPressionado(val digito: Int) : DetalheDoLancamentoEvent

    data object ApagarPressionado : DetalheDoLancamentoEvent

    data class TipoSelecionado(val tipo: TipoDoLancamentoManual) : DetalheDoLancamentoEvent

    data class AbrirPainel(val painel: PainelDoDetalhe) : DetalheDoLancamentoEvent

    data object FecharPainel : DetalheDoLancamentoEvent

    data class SelecionarInstrumento(val publicId: String) : DetalheDoLancamentoEvent

    data class SelecionarDestino(val publicId: String) : DetalheDoLancamentoEvent

    data class SelecionarCategoria(val publicId: String) : DetalheDoLancamentoEvent

    data object AbrirCalendario : DetalheDoLancamentoEvent

    data object FecharCalendario : DetalheDoLancamentoEvent

    data class SelecionarData(val data: LocalDate) : DetalheDoLancamentoEvent

    data class EstabelecimentoAlterado(val texto: String) : DetalheDoLancamentoEvent

    data class AplicarATodosAlterado(val aplicar: Boolean) : DetalheDoLancamentoEvent

    data object Salvar : DetalheDoLancamentoEvent

    data object SolicitarTransferencia : DetalheDoLancamentoEvent

    data object DesfazerTransferencia : DetalheDoLancamentoEvent

    data object SolicitarExclusao : DetalheDoLancamentoEvent

    data object CancelarExclusao : DetalheDoLancamentoEvent

    data object ConfirmarExclusao : DetalheDoLancamentoEvent

    data object DispensarMensagem : DetalheDoLancamentoEvent
}

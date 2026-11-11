package com.pingo.feature.transactions.impl

import java.time.LocalDate

sealed interface NovoLancamentoEvent {
    data class TipoSelecionado(val tipo: TipoDoLancamentoManual) : NovoLancamentoEvent

    data class DigitoPressionado(val digito: Int) : NovoLancamentoEvent

    data object ApagarPressionado : NovoLancamentoEvent

    data class EstabelecimentoAlterado(val texto: String) : NovoLancamentoEvent

    data class AbrirPainel(val painel: PainelDoNovoLancamento) : NovoLancamentoEvent

    data object FecharPainel : NovoLancamentoEvent

    data class SelecionarInstrumento(val publicId: String) : NovoLancamentoEvent

    data class SelecionarCategoria(val publicId: String) : NovoLancamentoEvent

    data object AbrirCalendario : NovoLancamentoEvent

    data object FecharCalendario : NovoLancamentoEvent

    data class SelecionarData(val data: LocalDate) : NovoLancamentoEvent

    data object Salvar : NovoLancamentoEvent
}

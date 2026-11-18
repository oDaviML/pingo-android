package com.pingo.feature.accounts.impl

sealed interface AccountsEvent {
    data class AbrirCompletarDatas(val instrumentoPublicId: String, val tituloCartao: String) : AccountsEvent

    data class AlterarFechamentoModal(val valor: String) : AccountsEvent

    data class AlterarVencimentoModal(val valor: String) : AccountsEvent

    data object SalvarDatasCartao : AccountsEvent

    data object FecharModalCompletarDatas : AccountsEvent

    data class AbrirAjustarSaldo(val instrumentoPublicId: String, val tituloVoucher: String, val saldoAtualCentavos: Long) : AccountsEvent

    data class AlterarModoAjuste(val modo: ModoAjusteVoucher) : AccountsEvent

    data class AlterarValorAjuste(val valor: String) : AccountsEvent

    data class AlterarObservacaoAjuste(val observacao: String) : AccountsEvent

    data object SalvarAjusteVoucher : AccountsEvent

    data object FecharModalAjustarSaldo : AccountsEvent
}

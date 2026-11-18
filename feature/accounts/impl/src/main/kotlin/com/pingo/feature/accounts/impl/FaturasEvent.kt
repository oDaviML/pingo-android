package com.pingo.feature.accounts.impl

import java.time.LocalDate

sealed interface FaturasEvent {
    data object TentarDeNovo : FaturasEvent

    data class SelecionarAba(val aba: AbaFaturas) : FaturasEvent

    data class SelecionarCiclo(val competencia: LocalDate) : FaturasEvent

    data object AbrirCompletarDatas : FaturasEvent

    data class AlterarFechamentoModal(val valor: String) : FaturasEvent

    data class AlterarVencimentoModal(val valor: String) : FaturasEvent

    data object SalvarDatasCartao : FaturasEvent

    data object FecharModalCompletarDatas : FaturasEvent
}

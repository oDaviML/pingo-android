package com.pingo.feature.reports.impl

import com.pingo.core.model.assinatura.ChaveAssinatura
import java.time.YearMonth

sealed interface ReportsEvent {
    data object MesAnterior : ReportsEvent

    data object MesProximo : ReportsEvent

    data class SelecionarMes(val mes: YearMonth) : ReportsEvent

    data object TentarDeNovo : ReportsEvent

    data class ConfirmarSugestao(val chave: ChaveAssinatura) : ReportsEvent

    data class RecusarSugestao(val chave: ChaveAssinatura) : ReportsEvent

    data class SolicitarCancelamentoAssinatura(val publicId: String) : ReportsEvent

    data object ConfirmarCancelamentoAssinatura : ReportsEvent

    data object DispensarCancelamentoAssinatura : ReportsEvent

    data object DispensarMensagemErro : ReportsEvent
}

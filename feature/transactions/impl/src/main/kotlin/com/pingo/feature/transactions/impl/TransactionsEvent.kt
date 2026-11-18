package com.pingo.feature.transactions.impl

import com.pingo.core.model.TipoLancamento
import java.time.YearMonth

sealed interface TransactionsEvent {
    data class BuscaAlterada(val texto: String) : TransactionsEvent

    data object AbrirFiltros : TransactionsEvent

    data object FecharFiltros : TransactionsEvent

    data class SelecionarMes(val mes: YearMonth?) : TransactionsEvent

    data class SelecionarTipo(val tipo: TipoLancamento?) : TransactionsEvent

    data class SelecionarVinculo(val vinculo: VinculoFiltro?) : TransactionsEvent

    data class SelecionarCategoria(val categoriaPublicId: String?) : TransactionsEvent

    data class SelecionarStatus(val status: StatusFiltro?) : TransactionsEvent

    data object LimparFiltros : TransactionsEvent

    data class Excluir(val publicId: String) : TransactionsEvent

    data object DesfazerExclusao : TransactionsEvent

    data object DispensarMensagem : TransactionsEvent

    data class AbrirTrocaCategoria(val publicId: String) : TransactionsEvent

    data object FecharTrocaCategoria : TransactionsEvent

    data class TrocarCategoria(val publicId: String, val categoriaPublicId: String) : TransactionsEvent

    data object ExportarCsv : TransactionsEvent

    data object ConsumirExportacaoCsv : TransactionsEvent

    data object DispensarAvisoExportacao : TransactionsEvent
}

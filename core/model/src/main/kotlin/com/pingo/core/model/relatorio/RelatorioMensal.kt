package com.pingo.core.model.relatorio

import androidx.compose.runtime.Immutable
import java.time.YearMonth

sealed interface VariacaoCategoria {
    @Immutable
    data object SemBase : VariacaoCategoria

    @Immutable
    data class Percentual(val deltaPercentual: Int) : VariacaoCategoria
}

@Immutable
data class ResumoCategoriaMensal(
    val categoriaPublicId: String?,
    val valorCentavos: Long,
    val valorMesAnteriorCentavos: Long,
    val percentualInteiro: Int,
    val pontosBase: Int,
    val variacaoMesAnterior: VariacaoCategoria,
)

@Immutable
data class PontoSerieMensal(val mes: YearMonth, val receitasCentavos: Long, val despesasCentavos: Long)

@Immutable
data class RelatorioMensal(
    val mes: YearMonth,
    val totalReceitasCentavos: Long,
    val totalDespesasCentavos: Long,
    val saldoMensalCentavos: Long,
    val categoriasDespesa: List<ResumoCategoriaMensal>,
    val serieSeisMeses: List<PontoSerieMensal>,
)

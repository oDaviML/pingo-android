package com.pingo.core.model.relatorio

import androidx.compose.runtime.Immutable
import java.time.YearMonth

@Immutable
data class ResumoCategoriaRelatorio(
    val categoriaPublicId: String?,
    val nomeCategoria: String,
    val valorCentavos: Long,
    val valorMesAnteriorCentavos: Long,
    val percentualInteiro: Int,
    val pontosBase: Int,
    val variacaoMesAnterior: VariacaoCategoria,
)

@Immutable
data class RelatorioMensalDetalhado(val relatorio: RelatorioMensal, val categorias: List<ResumoCategoriaRelatorio>, val mesAtual: YearMonth)

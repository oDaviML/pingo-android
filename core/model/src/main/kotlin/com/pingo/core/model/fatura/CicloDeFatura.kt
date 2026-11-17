package com.pingo.core.model.fatura

import androidx.compose.runtime.Immutable
import com.pingo.core.model.Fatura
import java.time.Instant
import java.time.LocalDate

@Immutable
data class CicloDeFatura(
    val competencia: LocalDate,
    val dataFechamento: LocalDate,
    val dataVencimento: LocalDate,
    val inicioInclusivo: Instant,
    val fimExclusivo: Instant,
    val fechamentoDia: Int,
    val vencimentoDia: Int,
)

sealed interface ResultadoCalculoCiclo {
    @Immutable
    data class CicloCalculado(val ciclo: CicloDeFatura, val vigenciaPublicId: String? = null) : ResultadoCalculoCiclo

    @Immutable
    data object ConfiguracaoIncompleta : ResultadoCalculoCiclo
}

sealed interface ResultadoResolucaoFatura {
    @Immutable
    data class FaturaExistente(val fatura: Fatura, val vigenciaPublicId: String? = null) : ResultadoResolucaoFatura

    @Immutable
    data class NovoCiclo(val ciclo: CicloDeFatura, val vigenciaPublicId: String? = null) : ResultadoResolucaoFatura

    @Immutable
    data object ConfiguracaoIncompleta : ResultadoResolucaoFatura
}

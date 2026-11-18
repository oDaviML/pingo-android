package com.pingo.core.model.assinatura

import androidx.compose.runtime.Immutable
import java.time.LocalDate

@Immutable
data class ChaveAssinatura(val instrumentoPublicId: String, val estabelecimentoNormalizado: String)

@Immutable
data class SugestaoDeAssinatura(
    val chave: ChaveAssinatura,
    val estabelecimentoExibicao: String,
    val valorReferenciaCentavos: Long,
    val ultimaCobranca: LocalDate,
    val proximaCobrancaEstimada: LocalDate,
    val lancamentosQualificadoresPublicIds: List<String>,
    val porCategoria: Boolean = false,
)

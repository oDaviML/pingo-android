package com.pingo.core.model

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.LocalDate

@Immutable
data class Assinatura(
    val publicId: String,
    val estabelecimento: String,
    val estabelecimentoNormalizado: String,
    val valorReferenciaCentavos: Long,
    val ultimaCobranca: LocalDate? = null,
    val proximaCobrancaEstimada: LocalDate? = null,
    val status: StatusAssinatura,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant? = null,
    val instrumentoPublicId: String? = null,
)

package com.pingo.core.model

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.LocalDate

@Immutable
data class Fatura(
    val publicId: String,
    val instrumentoPublicId: String,
    val competencia: LocalDate,
    val dataFechamento: LocalDate,
    val dataVencimento: LocalDate,
    val totalCentavos: Long,
    val totalPagoCentavos: Long = 0,
    val status: StatusFatura,
    val dataPagamento: LocalDate? = null,
    val pagamentoLancamentoPublicId: String? = null,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant? = null,
)

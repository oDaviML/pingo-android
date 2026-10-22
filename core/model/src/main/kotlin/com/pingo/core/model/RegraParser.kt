package com.pingo.core.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class RegraParser(
    val publicId: String,
    val instituicao: InstituicaoFinanceira,
    val versao: Int,
    val padroes: String,
    val ativa: Boolean,
    val publicadaEm: Instant,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
)

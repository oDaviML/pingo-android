package com.pingo.core.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class Conta(
    val publicId: String,
    val instituicao: InstituicaoFinanceira,
    val apelido: String,
    val tipo: TipoConta,
    val arquivada: Boolean = false,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant? = null,
)

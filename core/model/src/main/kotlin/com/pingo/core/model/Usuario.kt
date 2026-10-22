package com.pingo.core.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class Usuario(
    val publicId: String,
    val sub: String? = null,
    val email: String,
    val nome: String? = null,
    val consentimentoLgpdEm: Instant? = null,
    val consentimentoLgpdVersao: String? = null,
    val telemetriaOptIn: Boolean = false,
    val telemetriaOptInEm: Instant? = null,
    val aparelhoAtivoId: String? = null,
    val aparelhoAtivoNome: String? = null,
    val aparelhoAtivoEm: Instant? = null,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant? = null,
)

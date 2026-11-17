package com.pingo.core.model.fatura

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class VigenciaDeCartao(
    val publicId: String,
    val instrumentoPublicId: String,
    val vigenciaInicio: Instant,
    val fechamentoDia: Int?,
    val vencimentoDia: Int?,
    val excluidoEm: Instant? = null,
)

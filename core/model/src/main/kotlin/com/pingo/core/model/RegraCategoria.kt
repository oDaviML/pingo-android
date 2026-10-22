package com.pingo.core.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class RegraCategoria(
    val publicId: String,
    val categoriaPublicId: String,
    val estabelecimentoNormalizado: String,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant? = null,
)

package com.pingo.core.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class Categoria(
    val publicId: String,
    val nome: String,
    val tipo: TipoCategoria,
    val padrao: Boolean = false,
    val arquivada: Boolean = false,
    val criadoEm: Instant,
    val atualizadoEm: Instant,
    val excluidoEm: Instant? = null,
) {
    val podeSerEscolhida: Boolean
        get() = !arquivada && excluidoEm == null
}

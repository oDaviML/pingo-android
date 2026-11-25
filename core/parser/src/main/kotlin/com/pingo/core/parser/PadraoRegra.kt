package com.pingo.core.parser

import kotlinx.serialization.Serializable

@Serializable
data class PadraoRegra(
    val id: String,
    val tipoLancamento: TipoLancamentoPadrao? = null,
    val direcao: DirecaoPadrao? = null,
    val tipoInstrumento: TipoInstrumentoPadrao? = null,
    val ancoras: List<String> = emptyList(),
    val exclusoes: List<String> = emptyList(),
    val extracoes: Map<String, String> = emptyMap(),
    val ruido: Boolean = false,
)

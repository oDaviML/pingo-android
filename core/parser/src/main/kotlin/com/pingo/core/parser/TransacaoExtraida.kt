package com.pingo.core.parser

internal const val MOEDA_PADRAO = "BRL"

data class TransacaoExtraida(
    val padraoId: String,
    val tipo: TipoLancamentoPadrao,
    val direcao: DirecaoPadrao,
    val valorCentavos: Long,
    val estabelecimento: String?,
    val digitos: String?,
    val moeda: String = MOEDA_PADRAO,
    val saldoCentavos: Long? = null,
    val beneficio: String? = null,
)

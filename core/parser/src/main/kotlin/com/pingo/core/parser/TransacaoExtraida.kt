package com.pingo.core.parser

data class TransacaoExtraida(
    val padraoId: String,
    val tipo: TipoLancamentoPadrao,
    val direcao: DirecaoPadrao,
    val valorCentavos: Long,
    val estabelecimento: String?,
    val digitos: String?,
)

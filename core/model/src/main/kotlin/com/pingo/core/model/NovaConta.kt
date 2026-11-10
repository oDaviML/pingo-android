package com.pingo.core.model

data class NovaConta(
    val instituicao: InstituicaoFinanceira,
    val apelido: String,
    val tipo: TipoConta,
    val instrumentos: List<NovoInstrumento>,
) {
    fun valida(): Boolean = apelidoValido(apelido) && instrumentos.isNotEmpty() && instrumentos.all { it.valido() }
}

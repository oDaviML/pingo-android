package com.pingo.core.model

data class NovoInstrumento(val tipo: TipoInstrumento, val apelido: String, val ultimosDigitos: String? = null) {
    fun valido(): Boolean = apelidoValido(apelido) && ultimosDigitosValidos(ultimosDigitos)
}

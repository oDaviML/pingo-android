package com.pingo.core.model

const val TAMANHO_MAXIMO_APELIDO = 100

const val TAMANHO_ULTIMOS_DIGITOS = 4

fun apelidoValido(apelido: String): Boolean {
    val limpo = apelido.trim()

    return limpo.isNotEmpty() && limpo.length <= TAMANHO_MAXIMO_APELIDO
}

fun ultimosDigitosValidos(ultimosDigitos: String?): Boolean =
    ultimosDigitos == null || (ultimosDigitos.length == TAMANHO_ULTIMOS_DIGITOS && ultimosDigitos.all { it in '0'..'9' })

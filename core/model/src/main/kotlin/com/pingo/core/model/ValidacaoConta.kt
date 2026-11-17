package com.pingo.core.model

const val TAMANHO_MAXIMO_APELIDO = 100

const val TAMANHO_ULTIMOS_DIGITOS = 4

const val MENSAGEM_DIA_INVALIDO = "Informe um dia entre 1 e 31"

fun apelidoValido(apelido: String): Boolean {
    val limpo = apelido.trim()

    return limpo.isNotEmpty() && limpo.length <= TAMANHO_MAXIMO_APELIDO
}

fun ultimosDigitosValidos(ultimosDigitos: String?): Boolean =
    ultimosDigitos == null || (ultimosDigitos.length == TAMANHO_ULTIMOS_DIGITOS && ultimosDigitos.all { it in '0'..'9' })

fun diaDoMesValido(dia: Int?): Boolean = dia == null || dia in 1..31

fun diaDoMesInformado(dia: Int?): Boolean = dia != null && diaDoMesValido(dia)

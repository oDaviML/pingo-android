package com.pingo.core.designsystem.component

object MascaraMonetaria {
    private const val MAXIMO_DIGITOS = 11

    fun formatarCentavos(centavos: Long): String {
        val absoluto = kotlin.math.abs(centavos)
        val reais = absoluto / 100
        val resto = (absoluto % 100).toString().padStart(2, '0')
        val inteiro = reais.toString()
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()

        return "$inteiro,$resto"
    }

    fun formatarParaInput(centavos: Long, negativo: Boolean = false): String {
        val corpo = formatarCentavos(centavos)
        return if (negativo && centavos > 0L) "-$corpo" else corpo
    }

    fun extrairDigitosParaCampo(valorTexto: String): String {
        val texto = valorTexto.trim()
        val digitos = texto.filter { it.isDigit() }
        val resultado = if (texto.isEmpty() || digitos.isEmpty()) {
            ""
        } else {
            digitos.trimStart('0').ifEmpty { "0" }
        }
        return resultado
    }

    fun ehNegativo(valorTexto: String): Boolean = valorTexto.trimStart().startsWith("-")

    fun aplicarAlteracao(
        valorAtual: String,
        novaEntrada: String,
        permitirNegativo: Boolean = false,
        maximoDigitos: Int = MAXIMO_DIGITOS,
    ): String {
        val atualEhZero = valorAtual.trim() == "0,00"
        val negativo = resolverSinalNegativo(valorAtual, novaEntrada, permitirNegativo)
        val apenasDigitos = novaEntrada.filter { it.isDigit() }
        val limpouZero = atualEhZero && apenasDigitos == "0" && novaEntrada.length < extrairDigitosParaCampo(valorAtual).length
        val resultado = if (apenasDigitos.isEmpty() || limpouZero) {
            ""
        } else {
            val digitosLimpos = apenasDigitos.trimStart('0').take(maximoDigitos)
            val centavos = if (digitosLimpos.isEmpty()) 0L else digitosLimpos.toLongOrNull()
            centavos?.let { formatarParaInput(it, negativo) } ?: valorAtual
        }
        return resultado
    }

    fun alternarSinal(valorAtual: String): String {
        if (valorAtual.isBlank()) {
            return ""
        }
        val semSinal = valorAtual.trimStart().removePrefix("-").removePrefix("+").trimStart()
        return if (ehNegativo(valorAtual)) semSinal else "-$semSinal"
    }

    private fun resolverSinalNegativo(valorAtual: String, novaEntrada: String, permitirNegativo: Boolean): Boolean {
        val tinhaMenos = permitirNegativo && ehNegativo(valorAtual)
        val pediuMais = permitirNegativo && novaEntrada.contains('+')
        val pediuMenos = permitirNegativo && novaEntrada.contains('-')
        return when {
            pediuMais -> false
            pediuMenos -> true
            else -> tinhaMenos
        }
    }
}

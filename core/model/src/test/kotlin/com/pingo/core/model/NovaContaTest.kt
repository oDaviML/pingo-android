package com.pingo.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaContaTest {
    @Test
    fun `apelido com espacos nas pontas e valido depois de aparado`() {
        assertTrue(apelidoValido("  Nubank  "))
    }

    @Test
    fun `apelido so com espacos e invalido`() {
        assertFalse(apelidoValido("   "))
    }

    @Test
    fun `apelido com cem caracteres e valido`() {
        assertTrue(apelidoValido("a".repeat(TAMANHO_MAXIMO_APELIDO)))
    }

    @Test
    fun `apelido com cento e um caracteres e invalido`() {
        assertFalse(apelidoValido("a".repeat(TAMANHO_MAXIMO_APELIDO + 1)))
    }

    @Test
    fun `ultimos digitos ausentes sao validos`() {
        assertTrue(ultimosDigitosValidos(null))
    }

    @Test
    fun `ultimos digitos com quatro numeros sao validos`() {
        assertTrue(ultimosDigitosValidos("1234"))
    }

    @Test
    fun `ultimos digitos com menos ou mais de quatro numeros sao invalidos`() {
        assertFalse(ultimosDigitosValidos("123"))
        assertFalse(ultimosDigitosValidos("12345"))
    }

    @Test
    fun `ultimos digitos vazios ou com letras sao invalidos`() {
        assertFalse(ultimosDigitosValidos(""))
        assertFalse(ultimosDigitosValidos("12a4"))
    }

    @Test
    fun `conta sem instrumentos e invalida`() {
        assertFalse(novaConta(instrumentos = emptyList()).valida())
    }

    @Test
    fun `conta com apelido vazio e invalida`() {
        assertFalse(novaConta(apelido = "").valida())
    }

    @Test
    fun `conta com um instrumento invalido e invalida`() {
        val instrumentos = listOf(novoInstrumento(TipoInstrumento.CREDITO), novoInstrumento(apelido = " "))

        assertFalse(novaConta(instrumentos = instrumentos).valida())
    }

    @Test
    fun `instrumento sem apelido e invalido`() {
        assertFalse(novoInstrumento(apelido = " ").valido())
    }

    @Test
    fun `instrumento com ultimos digitos invalidos e invalido`() {
        assertFalse(novoInstrumento(ultimosDigitos = "12").valido())
    }

    @Test
    fun `conta com apelido e instrumentos validos e valida`() {
        assertTrue(novaConta().valida())
    }

    @Test
    fun `conta com dois instrumentos do mesmo tipo continua valida`() {
        val instrumentos = listOf(novoInstrumento(TipoInstrumento.DEBITO), novoInstrumento(TipoInstrumento.DEBITO))

        assertTrue(novaConta(instrumentos = instrumentos).valida())
    }

    private fun novaConta(
        apelido: String = "Nubank",
        instrumentos: List<NovoInstrumento> = listOf(novoInstrumento(TipoInstrumento.CREDITO)),
    ) = NovaConta(
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = apelido,
        tipo = TipoConta.CORRENTE,
        instrumentos = instrumentos,
    )

    private fun novoInstrumento(
        tipo: TipoInstrumento = TipoInstrumento.CREDITO,
        apelido: String = "Crédito",
        ultimosDigitos: String? = "1234",
    ) = NovoInstrumento(tipo = tipo, apelido = apelido, ultimosDigitos = ultimosDigitos, fechamentoDia = 5, vencimentoDia = 12)
}

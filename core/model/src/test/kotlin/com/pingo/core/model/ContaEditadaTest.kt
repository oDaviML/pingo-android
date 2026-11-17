package com.pingo.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContaEditadaTest {
    @Test
    fun `edicao com instrumento completo e valida`() {
        assertTrue(edicao().valida())
    }

    @Test
    fun `edicao exige ao menos um instrumento`() {
        assertFalse(edicao(instrumentos = emptyList()).valida())
    }

    @Test
    fun `edicao recusa identificador repetido`() {
        val repetido = listOf(
            instrumento(publicId = "instrumento-1"),
            instrumento(publicId = "instrumento-1"),
        )

        assertFalse(edicao(instrumentos = repetido).valida())
    }

    @Test
    fun `edicao aceita instrumento novo sem identificador`() {
        val novos = listOf(instrumento(publicId = null), instrumento(publicId = null))

        assertTrue(edicao(instrumentos = novos).valida())
    }

    @Test
    fun `edicao recusa credito sem fechamento ou vencimento`() {
        assertFalse(edicao(instrumentos = listOf(instrumento(fechamentoDia = null, vencimentoDia = null))).valida())
        assertFalse(edicao(instrumentos = listOf(instrumento(fechamentoDia = 0))).valida())
        assertFalse(edicao(instrumentos = listOf(instrumento(vencimentoDia = 32))).valida())
    }

    @Test
    fun `edicao recusa apelido em branco`() {
        assertFalse(edicao(apelido = "  ").valida())
        assertFalse(edicao(instrumentos = listOf(instrumento(apelido = " "))).valida())
    }

    private fun edicao(apelido: String = "Nubank", instrumentos: List<InstrumentoEditado> = listOf(instrumento())) = ContaEditada(
        publicId = "conta-1",
        apelido = apelido,
        tipo = TipoConta.CORRENTE,
        instrumentos = instrumentos,
    )

    private fun instrumento(
        publicId: String? = "instrumento-1",
        apelido: String = "Cartão",
        fechamentoDia: Int? = 5,
        vencimentoDia: Int? = 12,
    ) = InstrumentoEditado(
        tipo = TipoInstrumento.CREDITO,
        apelido = apelido,
        publicId = publicId,
        ultimosDigitos = "1234",
        fechamentoDia = fechamentoDia,
        vencimentoDia = vencimentoDia,
    )
}

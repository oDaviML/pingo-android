package com.pingo.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovoInstrumentoTest {
    @Test
    fun `credito exige fechamento e vencimento informados`() {
        assertFalse(credito().valido())
        assertFalse(credito(fechamentoDia = 5).valido())
        assertFalse(credito(vencimentoDia = 12).valido())
        assertTrue(credito(fechamentoDia = 5, vencimentoDia = 12).valido())
    }

    @Test
    fun `credito aceita os limites um e trinta e um`() {
        assertTrue(credito(fechamentoDia = 1, vencimentoDia = 1).valido())
        assertTrue(credito(fechamentoDia = 31, vencimentoDia = 31).valido())
    }

    @Test
    fun `credito recusa dia fora de um a trinta e um`() {
        assertFalse(credito(fechamentoDia = 0, vencimentoDia = 12).valido())
        assertFalse(credito(fechamentoDia = 32, vencimentoDia = 12).valido())
        assertFalse(credito(fechamentoDia = 5, vencimentoDia = 0).valido())
        assertFalse(credito(fechamentoDia = 5, vencimentoDia = 32).valido())
    }

    @Test
    fun `credito sem ultimos digitos continua valido`() {
        assertTrue(credito(fechamentoDia = 5, vencimentoDia = 12, ultimosDigitos = null).valido())
    }

    @Test
    fun `debito nao exige fechamento nem vencimento`() {
        assertTrue(NovoInstrumento(tipo = TipoInstrumento.DEBITO, apelido = "Débito").valido())
    }

    @Test
    fun `voucher exige beneficio e saldo inicial nao negativo`() {
        assertFalse(voucher().valido())
        assertFalse(voucher(beneficio = BeneficioVoucher.VR).valido())
        assertFalse(voucher(saldoCentavos = 2500).valido())
        assertFalse(voucher(beneficio = BeneficioVoucher.VR, saldoCentavos = -1).valido())
        assertTrue(voucher(beneficio = BeneficioVoucher.VR, saldoCentavos = 0).valido())
        assertTrue(voucher(beneficio = BeneficioVoucher.VA, saldoCentavos = 2500).valido())
        assertTrue(voucher(beneficio = BeneficioVoucher.VT, saldoCentavos = 2500).valido())
    }

    @Test
    fun `voucher sem recarga e valido`() {
        assertTrue(voucher(beneficio = BeneficioVoucher.VR, saldoCentavos = 2500).valido())
    }

    @Test
    fun `recarga exige dia e valor juntos e aceita os limites um e trinta e um`() {
        assertFalse(voucher(beneficio = BeneficioVoucher.VR, saldoCentavos = 2500, diaRecarga = 5).valido())
        assertFalse(voucher(beneficio = BeneficioVoucher.VR, saldoCentavos = 2500, valorRecargaCentavos = 50000).valido())
        assertFalse(
            voucher(beneficio = BeneficioVoucher.VR, saldoCentavos = 2500, diaRecarga = 0, valorRecargaCentavos = 50000).valido(),
        )
        assertFalse(
            voucher(beneficio = BeneficioVoucher.VR, saldoCentavos = 2500, diaRecarga = 32, valorRecargaCentavos = 50000).valido(),
        )
        assertTrue(
            voucher(beneficio = BeneficioVoucher.VR, saldoCentavos = 2500, diaRecarga = 1, valorRecargaCentavos = 50000).valido(),
        )
        assertTrue(
            voucher(beneficio = BeneficioVoucher.VT, saldoCentavos = 2500, diaRecarga = 31, valorRecargaCentavos = 50000).valido(),
        )
    }

    @Test
    fun `recarga com valor zero ou negativo e invalida`() {
        assertFalse(
            voucher(beneficio = BeneficioVoucher.VR, saldoCentavos = 2500, diaRecarga = 5, valorRecargaCentavos = 0).valido(),
        )
        assertFalse(
            voucher(beneficio = BeneficioVoucher.VR, saldoCentavos = 2500, diaRecarga = 5, valorRecargaCentavos = -100).valido(),
        )
    }

    @Test
    fun `apelido e ultimos digitos continuam obrigatorios`() {
        assertFalse(credito(fechamentoDia = 5, vencimentoDia = 12, apelido = " ").valido())
        assertFalse(credito(fechamentoDia = 5, vencimentoDia = 12, ultimosDigitos = "12").valido())
    }

    @Test
    fun `conta com credito sem datas e invalida e com datas completas e valida`() {
        val incompleta = NovaConta(
            instituicao = InstituicaoFinanceira.NUBANK,
            apelido = "Nubank",
            tipo = TipoConta.CORRENTE,
            instrumentos = listOf(credito()),
        )
        val completa = incompleta.copy(instrumentos = listOf(credito(fechamentoDia = 5, vencimentoDia = 12)))

        assertFalse(incompleta.valida())
        assertTrue(completa.valida())
    }

    @Test
    fun `conta com dois instrumentos do mesmo tipo aceita configuracoes distintas`() {
        val conta = NovaConta(
            instituicao = InstituicaoFinanceira.NUBANK,
            apelido = "Nubank",
            tipo = TipoConta.CORRENTE,
            instrumentos = listOf(
                credito(apelido = "Cartão principal", fechamentoDia = 5, vencimentoDia = 12, ultimosDigitos = "1234"),
                credito(apelido = "Cartão adicional", fechamentoDia = 20, vencimentoDia = 28, ultimosDigitos = "9876"),
            ),
        )

        assertTrue(conta.valida())
    }

    private fun credito(
        apelido: String = "Cartão de crédito",
        ultimosDigitos: String? = "1234",
        fechamentoDia: Int? = null,
        vencimentoDia: Int? = null,
    ) = NovoInstrumento(
        tipo = TipoInstrumento.CREDITO,
        apelido = apelido,
        ultimosDigitos = ultimosDigitos,
        fechamentoDia = fechamentoDia,
        vencimentoDia = vencimentoDia,
    )

    private fun voucher(
        beneficio: BeneficioVoucher? = null,
        saldoCentavos: Long? = null,
        diaRecarga: Int? = null,
        valorRecargaCentavos: Long? = null,
    ) = NovoInstrumento(
        tipo = TipoInstrumento.VOUCHER,
        apelido = "Voucher",
        beneficio = beneficio,
        saldoCentavos = saldoCentavos,
        diaRecarga = diaRecarga,
        valorRecargaCentavos = valorRecargaCentavos,
    )
}

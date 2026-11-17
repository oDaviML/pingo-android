package com.pingo.core.ui.contas

import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ContaFormularioTest {
    @Test
    fun `nova usa os padroes da instituicao`() {
        val caju = ContaFormulario.nova(InstituicaoFinanceira.CAJU)
        val itau = ContaFormulario.nova(InstituicaoFinanceira.ITAU)

        assertEquals(TipoConta.BENEFICIO, caju.tipoConta)
        assertEquals(listOf(TipoInstrumento.VOUCHER), caju.instrumentos.map { it.tipo })
        assertEquals("Caju", caju.apelido)
        assertEquals(TipoConta.CORRENTE, itau.tipoConta)
        assertEquals(listOf(TipoInstrumento.DEBITO), itau.instrumentos.map { it.tipo })
        assertFalse(caju.editando)
    }

    @Test
    fun `adiciona dois creditos com chaves e dados independentes`() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.NUBANK)
            .adicionarInstrumento(TipoInstrumento.CREDITO)
        val (primeiro, segundo) = formulario.instrumentos
        val alterado = formulario
            .aplicar(EventoDoFormularioDeConta.AlterarApelidoInstrumento(segundo.chave, "Cartão adicional"))
            .aplicar(EventoDoFormularioDeConta.AlterarUltimosDigitos(segundo.chave, "9876"))
            .aplicar(EventoDoFormularioDeConta.AlterarFechamento(primeiro.chave, "5"))
            .aplicar(EventoDoFormularioDeConta.AlterarVencimento(primeiro.chave, "12"))

        assertEquals(2, alterado.instrumentos.size)
        assertEquals("Cartão de crédito", alterado.instrumentos[0].apelido)
        assertEquals("", alterado.instrumentos[0].ultimosDigitos)
        assertEquals("5", alterado.instrumentos[0].fechamentoDia)
        assertEquals("Cartão adicional", alterado.instrumentos[1].apelido)
        assertEquals("9876", alterado.instrumentos[1].ultimosDigitos)
        assertEquals("", alterado.instrumentos[1].fechamentoDia)
        assertEquals(listOf("1", "2"), alterado.instrumentos.indices.map { alterado.numeroDoTipo(it).toString() })
    }

    @Test
    fun `remover mantem ao menos um instrumento`() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.NUBANK)
            .adicionarInstrumento(TipoInstrumento.DEBITO)
        val removido = formulario
            .aplicar(EventoDoFormularioDeConta.RemoverInstrumento(formulario.instrumentos.last().chave))
            .aplicar(EventoDoFormularioDeConta.RemoverInstrumento(formulario.instrumentos.first().chave))

        assertEquals(listOf(TipoInstrumento.CREDITO), removido.instrumentos.map { it.tipo })
    }

    @Test
    fun `credito aceita limites e recusa dias fora da faixa`() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.NUBANK)
        val chave = formulario.instrumentos.single().chave
        val valido = formulario
            .aplicar(EventoDoFormularioDeConta.AlterarFechamento(chave, "1"))
            .aplicar(EventoDoFormularioDeConta.AlterarVencimento(chave, "31"))

        assertTrue(valido.valida())
        assertTrue(valido.instrumentos.single().paraNovoInstrumento().ultimosDigitos == null)

        val zero = valido.aplicar(EventoDoFormularioDeConta.AlterarFechamento(chave, "0"))
        val trintaEDois = valido.aplicar(EventoDoFormularioDeConta.AlterarVencimento(chave, "32"))

        assertFalse(zero.valida())
        assertFalse(zero.instrumentos.single().fechamentoValido())
        assertFalse(trintaEDois.valida())
        assertFalse(trintaEDois.instrumentos.single().vencimentoValido())
    }

    @Test
    fun `vouchers mantem beneficio saldo e recarga independentes`() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.CAJU)
            .adicionarInstrumento(TipoInstrumento.VOUCHER)
        val primeiro = formulario.instrumentos[0]
        val segundo = formulario.instrumentos[1]
        val configurado = formulario
            .aplicar(EventoDoFormularioDeConta.AlterarBeneficio(primeiro.chave, BeneficioVoucher.VR))
            .aplicar(EventoDoFormularioDeConta.AlterarSaldoInicial(primeiro.chave, "486,20"))
            .aplicar(EventoDoFormularioDeConta.AlterarBeneficio(segundo.chave, BeneficioVoucher.VA))
            .aplicar(EventoDoFormularioDeConta.AlterarSaldoInicial(segundo.chave, "214,70"))
            .aplicar(EventoDoFormularioDeConta.AlterarDiaRecarga(segundo.chave, "5"))
            .aplicar(EventoDoFormularioDeConta.AlterarValorRecarga(segundo.chave, "300,00"))

        assertTrue(configurado.valida())
        assertEquals(BeneficioVoucher.VR, configurado.instrumentos[0].beneficio)
        assertEquals("486,20", configurado.instrumentos[0].saldoInicial)
        assertEquals("", configurado.instrumentos[0].diaRecarga)
        assertEquals(BeneficioVoucher.VA, configurado.instrumentos[1].beneficio)
        assertEquals("214,70", configurado.instrumentos[1].saldoInicial)
        assertEquals("5", configurado.instrumentos[1].diaRecarga)
        assertEquals("300,00", configurado.instrumentos[1].valorRecarga)
        assertEquals(48620L, configurado.instrumentos[0].paraNovoInstrumento().saldoCentavos)
        assertEquals(30000L, configurado.instrumentos[1].paraNovoInstrumento().valorRecargaCentavos)
    }

    @Test
    fun `voucher exige beneficio e saldo e recarga completa`() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.CAJU)
        val chave = formulario.instrumentos.single().chave

        assertFalse(formulario.valida())

        val semSaldo = formulario.aplicar(EventoDoFormularioDeConta.AlterarBeneficio(chave, BeneficioVoucher.VT))
        assertFalse(semSaldo.valida())

        val completo = semSaldo.aplicar(EventoDoFormularioDeConta.AlterarSaldoInicial(chave, "10,00"))
        assertTrue(completo.valida())

        val recargaSoDia = completo.aplicar(EventoDoFormularioDeConta.AlterarDiaRecarga(chave, "10"))
        assertFalse(recargaSoDia.valida())

        val recargaCompleta = recargaSoDia.aplicar(EventoDoFormularioDeConta.AlterarValorRecarga(chave, "50,00"))
        assertTrue(recargaCompleta.valida())
    }

    @Test
    fun `normaliza digitos dias e valores informados`() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.NUBANK)
        val chave = formulario.instrumentos.single().chave
        val normalizado = formulario
            .aplicar(EventoDoFormularioDeConta.AlterarUltimosDigitos(chave, "12a345"))
            .aplicar(EventoDoFormularioDeConta.AlterarFechamento(chave, "3x2"))

        assertEquals("1234", normalizado.instrumentos.single().ultimosDigitos)
        assertEquals("32", normalizado.instrumentos.single().fechamentoDia)

        val voucher = ContaFormulario.nova(InstituicaoFinanceira.CAJU)
        val chaveDoVoucher = voucher.instrumentos.single().chave
        val saldo = voucher.aplicar(EventoDoFormularioDeConta.AlterarSaldoInicial(chaveDoVoucher, "R$ 1.234,56"))

        assertEquals("1.234,56", saldo.instrumentos.single().saldoInicial)
        assertEquals(123456L, saldo.instrumentos.single().paraNovoInstrumento().saldoCentavos)
    }

    @Test
    fun `trocar instituicao atualiza apelido e instrumento padrao intocados`() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.NUBANK)
        val outro = formulario.aplicar(EventoDoFormularioDeConta.AlterarInstituicao(InstituicaoFinanceira.CAJU))

        assertEquals(InstituicaoFinanceira.CAJU, outro.instituicao)
        assertEquals("Caju", outro.apelido)
        assertEquals(listOf(TipoInstrumento.VOUCHER), outro.instrumentos.map { it.tipo })

        val personalizado = outro
            .aplicar(EventoDoFormularioDeConta.AlterarApelidoConta("Meu Caju"))
            .aplicar(EventoDoFormularioDeConta.AlterarInstituicao(InstituicaoFinanceira.ITAU))

        assertEquals("Meu Caju", personalizado.apelido)
        assertEquals(listOf(TipoInstrumento.DEBITO), personalizado.instrumentos.map { it.tipo })
    }

    @Test
    fun `de monta formulario de edicao preservando identificadores`() {
        val instante = Instant.parse("2026-11-10T12:00:00Z")
        val conta = Conta(
            publicId = "conta-1",
            instituicao = InstituicaoFinanceira.NUBANK,
            apelido = "Nubank pessoal",
            tipo = TipoConta.CORRENTE,
            criadoEm = instante,
            atualizadoEm = instante,
        )
        val instrumento = Instrumento(
            publicId = "instrumento-1",
            contaPublicId = "conta-1",
            tipo = TipoInstrumento.VOUCHER,
            apelido = "Caju VA",
            beneficio = BeneficioVoucher.VA,
            saldoCentavos = 21470L,
            diaRecarga = 5,
            valorRecargaCentavos = 30000L,
            criadoEm = instante,
            atualizadoEm = instante,
        )

        val formulario = ContaFormulario.de(conta, listOf(instrumento))
        val edicao = formulario.paraEdicao()

        assertTrue(formulario.editando)
        assertEquals("214,70", formulario.instrumentos.single().saldoInicial)
        assertEquals("300,00", formulario.instrumentos.single().valorRecarga)
        assertEquals("conta-1", edicao.publicId)
        assertEquals("instrumento-1", edicao.instrumentos.single().publicId)
        assertEquals(21470L, edicao.instrumentos.single().saldoCentavos)
    }

    @Test
    fun `debito sem digitos e valido e com digitos incompletos nao`() {
        val formulario = ContaFormulario.nova(InstituicaoFinanceira.ITAU)
        val chave = formulario.instrumentos.single().chave

        assertTrue(formulario.valida())

        val incompleto = formulario.aplicar(EventoDoFormularioDeConta.AlterarUltimosDigitos(chave, "12"))
        assertFalse(incompleto.valida())

        val completo = incompleto.aplicar(EventoDoFormularioDeConta.AlterarUltimosDigitos(chave, "1234"))
        assertTrue(completo.valida())
    }
}

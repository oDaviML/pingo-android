package com.pingo.core.model.voucher

import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class CalculadoraDeSaldoVoucherTest {
    private val t0: Instant = Instant.parse("2026-05-01T10:00:00Z")
    private val t1: Instant = Instant.parse("2026-05-02T12:00:00Z")
    private val t2: Instant = Instant.parse("2026-05-03T15:00:00Z")
    private val t3: Instant = Instant.parse("2026-05-04T18:00:00Z")

    private fun voucher(publicId: String, beneficio: BeneficioVoucher, saldoInicial: Long = 50_000L) = Instrumento(
        publicId = publicId,
        contaPublicId = "conta-caju",
        tipo = TipoInstrumento.VOUCHER,
        apelido = "Caju ${beneficio.name}",
        beneficio = beneficio,
        saldoCentavos = saldoInicial,
        criadoEm = t0,
        atualizadoEm = t0,
    )

    private fun movimento(
        publicId: String,
        instrumentoPublicId: String,
        valorCentavos: Long,
        dataHora: Instant,
        tipo: TipoLancamento = TipoLancamento.DESPESA,
        status: StatusLancamento = StatusLancamento.CONFIRMADO,
        excluidoEm: Instant? = null,
    ) = Lancamento(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        valorCentavos = valorCentavos,
        dataHora = dataHora,
        tipo = tipo,
        status = status,
        origem = OrigemLancamento.NOTIFICACAO,
        confianca = 1.0,
        criadoEm = dataHora,
        atualizadoEm = dataHora,
        excluidoEm = excluidoEm,
    )

    @Test
    fun `saldo explicito Caju representa estado apos propria transacao sem subtracao dupla`() {
        val vr = voucher("vr-1", BeneficioVoucher.VR, saldoInicial = 50_000L)
        val referencias = listOf(
            ReferenciaSaldoVoucher("ref-0", "vr-1", TipoReferenciaSaldoVoucher.ABERTURA, 50_000L, instanteReferencia = t0),
            ReferenciaSaldoVoucher(
                publicId = "ref-1",
                instrumentoPublicId = "vr-1",
                tipo = TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO,
                saldoCentavos = 45_000L,
                instanteReferencia = t1,
                lancamentoPublicId = "lanc-1",
            ),
        )
        val lancamentos = listOf(
            movimento("lanc-1", "vr-1", 5_000L, t1),
            movimento("lanc-2", "vr-1", 2_000L, t2),
        )

        val saldoAposCompra1 = CalculadoraDeSaldoVoucher.calcularSaldoCentavos(vr, referencias, listOf(lancamentos.first()))
        assertEquals(45_000L, saldoAposCompra1)

        val saldoFinal = CalculadoraDeSaldoVoucher.calcularSaldoCentavos(vr, referencias, lancamentos)
        assertEquals(43_000L, saldoFinal)
    }

    @Test
    fun `notificacao atrasada mais antiga nao substitui referencia mais recente e independe da ordem`() {
        val vr = voucher("vr-1", BeneficioVoucher.VR, saldoInicial = 50_000L)
        val refAntiga = ReferenciaSaldoVoucher(
            publicId = "ref-antiga",
            instrumentoPublicId = "vr-1",
            tipo = TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO,
            saldoCentavos = 48_000L,
            instanteReferencia = t0.plusSeconds(60),
            lancamentoPublicId = "lanc-0",
        )
        val refRecente = ReferenciaSaldoVoucher(
            publicId = "ref-recente",
            instrumentoPublicId = "vr-1",
            tipo = TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO,
            saldoCentavos = 45_000L,
            instanteReferencia = t2,
            lancamentoPublicId = "lanc-1",
        )
        val lanc0 = movimento("lanc-0", "vr-1", 2_000L, t0.plusSeconds(60))
        val lanc1 = movimento("lanc-1", "vr-1", 5_000L, t2)
        val lanc2 = movimento("lanc-2", "vr-1", 1_500L, t3)

        val ordemDireta = CalculadoraDeSaldoVoucher.calcularSaldoCentavos(
            instrumento = vr,
            referencias = listOf(refAntiga, refRecente),
            lancamentos = listOf(lanc0, lanc1, lanc2),
        )
        val ordemInvertida = CalculadoraDeSaldoVoucher.calcularSaldoCentavos(
            instrumento = vr,
            referencias = listOf(refRecente, refAntiga),
            lancamentos = listOf(lanc2, lanc1, lanc0),
        )

        assertEquals(43_500L, ordemDireta)
        assertEquals(ordemDireta, ordemInvertida)
    }

    @Test
    fun `ajuste define saldo alvo naquele instante e considera movimentos posteriores sem contar receita ou despesa`() {
        val va = voucher("va-1", BeneficioVoucher.VA, saldoInicial = 30_000L)
        val lancAntes = movimento("l-1", "va-1", 5_000L, t1)
        val lancDepois = movimento("l-3", "va-1", 3_000L, t3)
        val lancAjuste = movimento("l-2", "va-1", 15_000L, t2, tipo = TipoLancamento.AJUSTE)
        val refAbertura = ReferenciaSaldoVoucher("r-0", "va-1", TipoReferenciaSaldoVoucher.ABERTURA, 30_000L, instanteReferencia = t0)

        val ajusteCalculado = CalculadoraDeSaldoVoucher.calcularAjuste(
            instrumento = va,
            referencias = listOf(refAbertura),
            lancamentos = listOf(lancAntes),
            saldoAlvoCentavos = 40_000L,
            instanteAjuste = t2,
        )
        assertEquals(25_000L, ajusteCalculado.saldoAnteriorCentavos)
        assertEquals(40_000L, ajusteCalculado.saldoAlvoCentavos)
        assertEquals(15_000L, ajusteCalculado.deltaCentavos)
        assertEquals(15_000L, ajusteCalculado.valorLancamentoCentavos)

        val refAjuste = ReferenciaSaldoVoucher(
            publicId = "r-ajuste",
            instrumentoPublicId = "va-1",
            tipo = TipoReferenciaSaldoVoucher.AJUSTE,
            saldoCentavos = 40_000L,
            deltaCentavos = 15_000L,
            instanteReferencia = t2,
            lancamentoPublicId = "l-2",
        )
        val saldoAposAjusteEDepois = CalculadoraDeSaldoVoucher.calcularSaldoCentavos(
            instrumento = va,
            referencias = listOf(refAbertura, refAjuste),
            lancamentos = listOf(lancAntes, lancAjuste, lancDepois),
        )
        assertEquals(37_000L, saldoAposAjusteEDepois)
        assertEquals(0L, CalculadoraDeSaldoVoucher.efeitoLancamentoNoSaldo(lancAjuste))
    }

    @Test
    fun `vr va e vt permanecem isolados e pendentes ou excluidos nao alteram saldo confirmado`() {
        val vr = voucher("vr-1", BeneficioVoucher.VR, saldoInicial = 10_000L)
        val va = voucher("va-1", BeneficioVoucher.VA, saldoInicial = 20_000L)
        val lancamentos = listOf(
            movimento("l-vr", "vr-1", 2_000L, t1),
            movimento("l-vr-pend", "vr-1", 1_000L, t2, status = StatusLancamento.PENDENTE),
            movimento("l-vr-excl", "vr-1", 1_000L, t2, excluidoEm = t3),
            movimento("l-vr-transf", "vr-1", 5_000L, t2, tipo = TipoLancamento.TRANSFERENCIA),
            movimento("l-va", "va-1", 4_000L, t1),
        )

        assertEquals(8_000L, CalculadoraDeSaldoVoucher.calcularSaldoCentavos(vr, emptyList(), lancamentos))
        assertEquals(16_000L, CalculadoraDeSaldoVoucher.calcularSaldoCentavos(va, emptyList(), lancamentos))
    }

    @Test
    fun `exclusao de lancamento invalida referencia vinculada a ele`() {
        val vr = voucher("vr-1", BeneficioVoucher.VR, saldoInicial = 50_000L)
        val refAbertura = ReferenciaSaldoVoucher("r-0", "vr-1", TipoReferenciaSaldoVoucher.ABERTURA, 50_000L, instanteReferencia = t0)
        val refObservada = ReferenciaSaldoVoucher(
            publicId = "r-1",
            instrumentoPublicId = "vr-1",
            tipo = TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO,
            saldoCentavos = 45_000L,
            instanteReferencia = t1,
            lancamentoPublicId = "l-1",
        )
        val lancExcluido = movimento("l-1", "vr-1", 5_000L, t1, excluidoEm = t2)

        assertEquals(
            50_000L,
            CalculadoraDeSaldoVoucher.calcularSaldoCentavos(vr, listOf(refAbertura, refObservada), listOf(lancExcluido)),
        )
    }
}

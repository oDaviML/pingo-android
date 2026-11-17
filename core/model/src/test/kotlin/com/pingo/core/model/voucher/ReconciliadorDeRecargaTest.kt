package com.pingo.core.model.voucher

import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Instrumento
import com.pingo.core.model.TipoInstrumento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class ReconciliadorDeRecargaTest {
    private val t0: Instant = Instant.parse("2026-01-01T09:00:00Z")

    private fun voucherComRecarga(
        publicId: String = "vr-1",
        beneficio: BeneficioVoucher = BeneficioVoucher.VR,
        diaRecarga: Int? = 31,
        valorCentavos: Long? = 80_000L,
        arquivado: Boolean = false,
    ) = Instrumento(
        publicId = publicId,
        contaPublicId = "conta-1",
        tipo = TipoInstrumento.VOUCHER,
        apelido = "VR Caju",
        beneficio = beneficio,
        saldoCentavos = 0L,
        diaRecarga = diaRecarga,
        valorRecargaCentavos = valorCentavos,
        arquivado = arquivado,
        criadoEm = t0,
        atualizadoEm = t0,
    )

    private fun instanteSaoPaulo(ano: Int, mes: Int, dia: Int, hora: Int = 10): Instant = LocalDateTime.of(ano, mes, dia, hora, 0)
        .atZone(ReconciliadorDeRecarga.ZONA_SAO_PAULO)
        .toInstant()

    @Test
    fun `ocorrencias devidas respeitam dia configurado com clamp em fevereiro e nao antecipam meses futuros`() {
        val vr = voucherComRecarga(diaRecarga = 31, valorCentavos = 80_000L)
        val inicio = LocalDate.of(2026, 1, 1)

        val antesDeVencerJan = ReconciliadorDeRecarga.gerarOcorrenciasDevidas(vr, inicio, LocalDate.of(2026, 1, 30), emptyList())
        assertTrue(antesDeVencerJan.isEmpty())

        val ateFev28 = ReconciliadorDeRecarga.gerarOcorrenciasDevidas(vr, inicio, LocalDate.of(2026, 2, 28), emptyList())
        assertEquals(2, ateFev28.size)
        assertEquals(LocalDate.of(2026, 1, 31), ateFev28[0].dataPrevista)
        assertEquals(LocalDate.of(2026, 2, 28), ateFev28[1].dataPrevista)

        val jaExistente = RecargaProgramada(
            publicId = "rec-jan",
            instrumentoPublicId = "vr-1",
            beneficio = BeneficioVoucher.VR,
            competenciaMes = LocalDate.of(2026, 1, 1),
            dataPrevista = LocalDate.of(2026, 1, 31),
            valorPrevistoCentavos = 80_000L,
            status = StatusRecargaProgramada.SUBSTITUIDA,
        )
        val idempotente = ReconciliadorDeRecarga.gerarOcorrenciasDevidas(
            instrumento = vr,
            desdeDataInclusiva = inicio,
            hoje = LocalDate.of(2026, 2, 28),
            existentes = listOf(jaExistente),
        )
        assertEquals(1, idempotente.size)
        assertEquals(LocalDate.of(2026, 2, 1), idempotente.single().competenciaMes)
    }

    @Test
    fun `conciliacao automatica exige correspondencia unica de instrumento beneficio valor e dia local`() {
        val pendente = RecargaProgramada(
            publicId = "rec-1",
            instrumentoPublicId = "vr-1",
            beneficio = BeneficioVoucher.VR,
            competenciaMes = LocalDate.of(2026, 5, 1),
            dataPrevista = LocalDate.of(2026, 5, 5),
            valorPrevistoCentavos = 80_000L,
            status = StatusRecargaProgramada.PENDENTE,
        )
        val depositoExato = DepositoVoucherCapturado(
            instrumentoPublicId = "vr-1",
            beneficio = BeneficioVoucher.VR,
            valorCentavos = 80_000L,
            dataHora = instanteSaoPaulo(2026, 5, 5, 14),
        )
        val resultado = ReconciliadorDeRecarga.conciliarDeposito(depositoExato, listOf(pendente))
        assertEquals(ResultadoReconciliacaoRecarga.SubstituicaoUnica(pendente), resultado)
    }

    @Test
    fun `beneficio ausente outro dia outro valor ou multiplos candidatos exigem revisao`() {
        val pendenteVr = RecargaProgramada(
            publicId = "rec-vr",
            instrumentoPublicId = "vr-1",
            beneficio = BeneficioVoucher.VR,
            competenciaMes = LocalDate.of(2026, 5, 1),
            dataPrevista = LocalDate.of(2026, 5, 5),
            valorPrevistoCentavos = 80_000L,
            status = StatusRecargaProgramada.PENDENTE,
        )
        val semBeneficio = DepositoVoucherCapturado("vr-1", null, 80_000L, instanteSaoPaulo(2026, 5, 5))
        val resAmbiguo = ReconciliadorDeRecarga.conciliarDeposito(semBeneficio, listOf(pendenteVr))
            as ResultadoReconciliacaoRecarga.RequerRevisao
        assertEquals(MotivoRevisaoRecarga.BENEFICIO_AMBIGUO, resAmbiguo.motivo)

        val outroDia = DepositoVoucherCapturado("vr-1", BeneficioVoucher.VR, 80_000L, instanteSaoPaulo(2026, 5, 6))
        val resOutroDia = ReconciliadorDeRecarga.conciliarDeposito(outroDia, listOf(pendenteVr))
            as ResultadoReconciliacaoRecarga.RequerRevisao
        assertEquals(MotivoRevisaoRecarga.DIVERGENCIA_RECARGA, resOutroDia.motivo)

        val outroValor = DepositoVoucherCapturado("vr-1", BeneficioVoucher.VR, 75_000L, instanteSaoPaulo(2026, 5, 5))
        val resOutroValor = ReconciliadorDeRecarga.conciliarDeposito(outroValor, listOf(pendenteVr))
            as ResultadoReconciliacaoRecarga.RequerRevisao
        assertEquals(MotivoRevisaoRecarga.DIVERGENCIA_RECARGA, resOutroValor.motivo)

        val pendenteDuplicado = pendenteVr.copy(publicId = "rec-vr-2")
        val depositoExato = DepositoVoucherCapturado("vr-1", BeneficioVoucher.VR, 80_000L, instanteSaoPaulo(2026, 5, 5))
        val resMultiplos = ReconciliadorDeRecarga.conciliarDeposito(depositoExato, listOf(pendenteVr, pendenteDuplicado))
            as ResultadoReconciliacaoRecarga.RequerRevisao
        assertEquals(MotivoRevisaoRecarga.DIVERGENCIA_RECARGA, resMultiplos.motivo)
        assertEquals(2, resMultiplos.candidatos.size)

        val outroBeneficio = DepositoVoucherCapturado("va-1", BeneficioVoucher.VA, 80_000L, instanteSaoPaulo(2026, 5, 5))
        assertEquals(
            ResultadoReconciliacaoRecarga.SemPrevisaoCompativel,
            ReconciliadorDeRecarga.conciliarDeposito(outroBeneficio, listOf(pendenteVr)),
        )
    }
}

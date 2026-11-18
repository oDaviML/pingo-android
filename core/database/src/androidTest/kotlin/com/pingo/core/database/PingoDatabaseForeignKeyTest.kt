package com.pingo.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.database.entity.AssinaturaEntity
import com.pingo.core.database.entity.ConfiguracaoCartaoEntity
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.FaturaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import com.pingo.core.database.entity.ReferenciaSaldoVoucherEntity
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.voucher.StatusRecargaProgramada
import com.pingo.core.model.voucher.TipoReferenciaSaldoVoucher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PingoDatabaseForeignKeyTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: PingoDatabase

    @Before
    fun setUp() {
        TestPingoDatabase.delete(context)
        database = TestPingoDatabase.open(context)
    }

    @After
    fun tearDown() {
        database.close()
        TestPingoDatabase.delete(context)
    }

    @Test
    fun referenceToMissingParentIsRejected() = runBlocking {
        val falhou = runCatching {
            database.instrumentoDao().upsert(instrumento())
        }.isFailure

        assertTrue(falhou)
    }

    @Test
    fun referenceToExistingParentIsAccepted() = runBlocking {
        database.contaDao().upsert(conta())
        database.instrumentoDao().upsert(instrumento())

        assertNotNull(database.instrumentoDao().buscar(INSTRUMENTO_ID))
    }

    @Test
    fun pendingCaptureWithoutInstrumentIsAccepted() = runBlocking {
        database.lancamentoDao().upsert(lancamento().copy(instrumentoPublicId = null, faturaPublicId = null))

        assertNotNull(database.lancamentoDao().buscar(LANCAMENTO_ID))
    }

    @Test
    fun pendingCaptureWithMissingInstrumentIsRejected() = runBlocking {
        val falhou = runCatching {
            database.lancamentoDao().upsert(
                lancamento().copy(instrumentoPublicId = "instrumento-inexistente", faturaPublicId = null),
            )
        }.isFailure

        assertTrue(falhou)
    }

    @Test
    fun invoiceCanReferenceAPaymentSavedAfterIt() = runBlocking {
        database.contaDao().upsert(conta())
        database.instrumentoDao().upsert(instrumento())
        database.faturaDao().upsert(fatura())
        database.lancamentoDao().upsert(lancamento())

        assertNotNull(database.lancamentoDao().buscar(LANCAMENTO_ID))
        assertEquals(LANCAMENTO_ID, database.faturaDao().buscar(FATURA_ID)?.pagamentoLancamentoPublicId)
    }

    @Test
    fun configuracaoCartaoRequiresExistingInstrument() = runBlocking {
        val cfg = ConfiguracaoCartaoEntity(
            publicId = "cfg-1",
            instrumentoPublicId = INSTRUMENTO_ID,
            vigenciaInicio = INSTANTE,
            fechamentoDia = 5,
            vencimentoDia = 12,
            criadoEm = INSTANTE,
            atualizadoEm = INSTANTE,
            excluidoEm = null,
        )
        assertTrue(runCatching { database.configuracaoCartaoDao().upsert(cfg) }.isFailure)

        database.contaDao().upsert(conta())
        database.instrumentoDao().upsert(instrumento())
        database.configuracaoCartaoDao().upsert(cfg)
        assertNotNull(database.configuracaoCartaoDao().buscar("cfg-1"))
    }

    @Test
    fun voucherTablesValidateInstrumentAndLancamentoForeignKeys() = runBlocking {
        database.contaDao().upsert(conta())
        database.instrumentoDao().upsert(instrumento())

        val refInvalida = ReferenciaSaldoVoucherEntity(
            publicId = "ref-bad",
            instrumentoPublicId = INSTRUMENTO_ID,
            tipoReferencia = TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO,
            saldoCentavos = 10000L,
            deltaCentavos = null,
            instanteReferencia = INSTANTE,
            lancamentoPublicId = "lanc-inexistente",
            criadoEm = INSTANTE,
            atualizadoEm = INSTANTE,
            excluidoEm = null,
        )
        assertTrue(runCatching { database.referenciaSaldoVoucherDao().upsert(refInvalida) }.isFailure)

        val recInvalida = RecargaProgramadaVoucherEntity(
            publicId = "rec-bad",
            instrumentoPublicId = "inst-inexistente",
            beneficio = BeneficioVoucher.VA,
            competenciaMes = LocalDate.of(2026, 11, 1),
            dataPrevista = LocalDate.of(2026, 11, 5),
            valorPrevistoCentavos = 50000L,
            status = StatusRecargaProgramada.PENDENTE,
            lancamentoPublicId = null,
            criadoEm = INSTANTE,
            atualizadoEm = INSTANTE,
            excluidoEm = null,
        )
        assertTrue(runCatching { database.recargaProgramadaVoucherDao().upsert(recInvalida) }.isFailure)

        val recSemLanc = recInvalida.copy(publicId = "rec-ok", instrumentoPublicId = INSTRUMENTO_ID)
        database.recargaProgramadaVoucherDao().upsert(recSemLanc)
        assertNotNull(database.recargaProgramadaVoucherDao().buscar("rec-ok"))
    }

    @Test
    fun assinaturaAllowsNullInstrumentAndValidatesNonNullInstrument() = runBlocking {
        val subNull = AssinaturaEntity(
            publicId = "sub-null",
            estabelecimento = "Netflix",
            estabelecimentoNormalizado = "NETFLIX",
            valorReferenciaCentavos = 5590L,
            ultimaCobranca = null,
            proximaCobrancaEstimada = null,
            status = StatusAssinatura.SUGERIDA,
            criadoEm = INSTANTE,
            atualizadoEm = INSTANTE,
            excluidoEm = null,
            instrumentoPublicId = null,
        )
        database.assinaturaDao().upsert(subNull)
        assertNotNull(database.assinaturaDao().buscar("sub-null"))

        val subBad = subNull.copy(publicId = "sub-bad", instrumentoPublicId = "inst-inexistente")
        assertTrue(runCatching { database.assinaturaDao().upsert(subBad) }.isFailure)

        database.contaDao().upsert(conta())
        database.instrumentoDao().upsert(instrumento())
        database.assinaturaDao().upsert(subNull.copy(publicId = "sub-ok", instrumentoPublicId = INSTRUMENTO_ID))
        assertNotNull(database.assinaturaDao().buscar("sub-ok"))
    }

    private fun conta() = ContaEntity(
        publicId = CONTA_ID,
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = "Conta",
        tipo = TipoConta.CORRENTE,
        arquivada = false,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
        excluidoEm = null,
    )

    private fun instrumento() = InstrumentoEntity(
        publicId = INSTRUMENTO_ID,
        contaPublicId = CONTA_ID,
        tipo = TipoInstrumento.DEBITO,
        apelido = "Débito",
        ultimosDigitos = null,
        arquivado = false,
        fechamentoDia = null,
        vencimentoDia = null,
        beneficio = null,
        saldoCentavos = null,
        diaRecarga = null,
        valorRecargaCentavos = null,
        limiteCreditoCentavos = null,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
        excluidoEm = null,
    )

    private fun fatura() = FaturaEntity(
        publicId = FATURA_ID,
        instrumentoPublicId = INSTRUMENTO_ID,
        competencia = LocalDate.of(2026, 10, 1),
        dataFechamento = LocalDate.of(2026, 10, 5),
        dataVencimento = LocalDate.of(2026, 10, 12),
        totalCentavos = 100,
        totalPagoCentavos = 0,
        status = StatusFatura.ABERTA,
        dataPagamento = null,
        pagamentoLancamentoPublicId = LANCAMENTO_ID,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
        excluidoEm = null,
    )

    private fun lancamento() = LancamentoEntity(
        publicId = LANCAMENTO_ID,
        instrumentoPublicId = INSTRUMENTO_ID,
        categoriaPublicId = null,
        faturaPublicId = FATURA_ID,
        assinaturaPublicId = null,
        compraPaiPublicId = null,
        estornoDePublicId = null,
        transferenciaId = null,
        valorCentavos = 100,
        dataHora = INSTANTE,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = "Estabelecimento",
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.MANUAL,
        confianca = 1.0,
        motivoPendencia = null,
        hashDuplicata = null,
        observacao = null,
        parcelaNumero = 1,
        totalParcelas = 1,
        valorTotalCompraCentavos = null,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
        excluidoEm = null,
        textoBruto = null,
    )

    private companion object {
        const val CONTA_ID = "conta-1"
        const val INSTRUMENTO_ID = "instrumento-1"
        const val FATURA_ID = "fatura-1"
        const val LANCAMENTO_ID = "lancamento-1"
        val INSTANTE: Instant = Instant.parse("2026-10-22T12:00:00Z")
    }
}

package com.pingo.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.FaturaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
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
    fun invoiceCanReferenceAPaymentSavedAfterIt() = runBlocking {
        database.contaDao().upsert(conta())
        database.instrumentoDao().upsert(instrumento())
        database.faturaDao().upsert(fatura())
        database.lancamentoDao().upsert(lancamento())

        assertNotNull(database.lancamentoDao().buscar(LANCAMENTO_ID))
        assertEquals(LANCAMENTO_ID, database.faturaDao().buscar(FATURA_ID)?.pagamentoLancamentoPublicId)
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

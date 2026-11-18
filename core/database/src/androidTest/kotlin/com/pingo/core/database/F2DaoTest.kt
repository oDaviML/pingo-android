package com.pingo.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.database.entity.AssinaturaEntity
import com.pingo.core.database.entity.ConfiguracaoCartaoEntity
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import com.pingo.core.database.entity.ReferenciaSaldoVoucherEntity
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.voucher.StatusRecargaProgramada
import com.pingo.core.model.voucher.TipoReferenciaSaldoVoucher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class F2DaoTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: PingoDatabase

    @Before
    fun setUp() = runBlocking {
        TestPingoDatabase.delete(context)
        database = TestPingoDatabase.open(context)
        database.contaDao().upsert(
            ContaEntity(
                publicId = "conta-1",
                instituicao = InstituicaoFinanceira.CAJU,
                apelido = "Caju",
                tipo = TipoConta.BENEFICIO,
                arquivada = false,
                criadoEm = INSTANTE,
                atualizadoEm = INSTANTE,
                excluidoEm = null,
            ),
        )
        database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = "inst-cartao",
                contaPublicId = "conta-1",
                tipo = TipoInstrumento.CREDITO,
                apelido = "Cartão",
                ultimosDigitos = "1111",
                arquivado = false,
                fechamentoDia = 5,
                vencimentoDia = 12,
                beneficio = null,
                saldoCentavos = null,
                diaRecarga = null,
                valorRecargaCentavos = null,
                limiteCreditoCentavos = null,
                criadoEm = INSTANTE,
                atualizadoEm = INSTANTE,
                excluidoEm = null,
            ),
        )
        database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = "inst-voucher",
                contaPublicId = "conta-1",
                tipo = TipoInstrumento.VOUCHER,
                apelido = "VA",
                ultimosDigitos = null,
                arquivado = false,
                fechamentoDia = null,
                vencimentoDia = null,
                beneficio = BeneficioVoucher.VA,
                saldoCentavos = 50000L,
                diaRecarga = 5,
                valorRecargaCentavos = 60000L,
                limiteCreditoCentavos = null,
                criadoEm = INSTANTE,
                atualizadoEm = INSTANTE,
                excluidoEm = null,
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
        TestPingoDatabase.delete(context)
    }

    @Test
    fun configuracaoCartaoDaoOrdersByVigenciaAndFiltersDeleted() = runBlocking {
        val dao = database.configuracaoCartaoDao()
        dao.upsertAll(
            listOf(
                ConfiguracaoCartaoEntity("cfg-2", "inst-cartao", INSTANTE.plusSeconds(100), 10, 17, INSTANTE, INSTANTE, null),
                ConfiguracaoCartaoEntity("cfg-1", "inst-cartao", INSTANTE, 5, 12, INSTANTE, INSTANTE, null),
                ConfiguracaoCartaoEntity("cfg-del", "inst-cartao", INSTANTE.plusSeconds(200), 15, 22, INSTANTE, INSTANTE, INSTANTE),
            ),
        )

        val vigencias = dao.listarPorInstrumento("inst-cartao")
        assertEquals(listOf("cfg-1", "cfg-2"), vigencias.map { it.publicId })

        val atualizadas = dao.atualizadasDesde(INSTANTE)
        assertEquals(3, atualizadas.size)
    }

    @Test
    fun referenciaERecargaDaoRepointLancamentoOnAliasMerge() = runBlocking {
        database.lancamentoDao().upsertAll(
            listOf(
                lancamento("lanc-dup", INSTANTE),
                lancamento("lanc-dono", INSTANTE),
            ),
        )
        val refDao = database.referenciaSaldoVoucherDao()
        val recDao = database.recargaProgramadaVoucherDao()
        refDao.upsert(
            ReferenciaSaldoVoucherEntity(
                publicId = "ref-1",
                instrumentoPublicId = "inst-voucher",
                tipoReferencia = TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO,
                saldoCentavos = 45000L,
                deltaCentavos = null,
                instanteReferencia = INSTANTE,
                lancamentoPublicId = "lanc-dup",
                criadoEm = INSTANTE,
                atualizadoEm = INSTANTE,
                excluidoEm = null,
            ),
        )
        recDao.upsert(
            RecargaProgramadaVoucherEntity(
                publicId = "rec-1",
                instrumentoPublicId = "inst-voucher",
                beneficio = BeneficioVoucher.VA,
                competenciaMes = LocalDate.of(2026, 11, 1),
                dataPrevista = LocalDate.of(2026, 11, 5),
                valorPrevistoCentavos = 60000L,
                status = StatusRecargaProgramada.SUBSTITUIDA,
                lancamentoPublicId = "lanc-dup",
                criadoEm = INSTANTE,
                atualizadoEm = INSTANTE,
                excluidoEm = null,
            ),
        )

        refDao.repointarLancamento("lanc-dup", "lanc-dono")
        recDao.repointarLancamento("lanc-dup", "lanc-dono")

        assertEquals("lanc-dono", refDao.buscarPorLancamento("lanc-dono")?.publicId?.let { refDao.buscar(it)?.lancamentoPublicId })
        assertNull(refDao.buscarPorLancamento("lanc-dup"))
        assertEquals(listOf("rec-1"), recDao.buscarPorLancamento("lanc-dono").map { it.publicId })
    }

    @Test
    fun assinaturaDaoSeparatesByInstrumentWhilePreservingLegacyNullKey() = runBlocking {
        val dao = database.assinaturaDao()
        dao.upsertAll(
            listOf(
                AssinaturaEntity(
                    publicId = "sub-legacy",
                    estabelecimento = "Spotify",
                    estabelecimentoNormalizado = "SPOTIFY",
                    valorReferenciaCentavos = 2190L,
                    ultimaCobranca = LocalDate.of(2026, 10, 1),
                    proximaCobrancaEstimada = LocalDate.of(2026, 11, 1),
                    status = StatusAssinatura.CONFIRMADA,
                    criadoEm = INSTANTE,
                    atualizadoEm = INSTANTE,
                    excluidoEm = null,
                    instrumentoPublicId = null,
                ),
                AssinaturaEntity(
                    publicId = "sub-cartao",
                    estabelecimento = "Spotify",
                    estabelecimentoNormalizado = "SPOTIFY",
                    valorReferenciaCentavos = 2190L,
                    ultimaCobranca = LocalDate.of(2026, 11, 1),
                    proximaCobrancaEstimada = LocalDate.of(2026, 12, 1),
                    status = StatusAssinatura.SUGERIDA,
                    criadoEm = INSTANTE,
                    atualizadoEm = INSTANTE,
                    excluidoEm = null,
                    instrumentoPublicId = "inst-cartao",
                ),
            ),
        )

        assertEquals("sub-legacy", dao.buscarLegadaPorEstabelecimento("SPOTIFY")?.publicId)
        assertEquals("sub-cartao", dao.buscarPorChaveComInstrumento("inst-cartao", "SPOTIFY")?.publicId)
    }

    private fun lancamento(publicId: String, instante: Instant) = LancamentoEntity(
        publicId = publicId,
        instrumentoPublicId = "inst-voucher",
        categoriaPublicId = null,
        faturaPublicId = null,
        assinaturaPublicId = null,
        compraPaiPublicId = null,
        estornoDePublicId = null,
        transferenciaId = null,
        valorCentavos = 5000L,
        dataHora = instante,
        tipo = TipoLancamento.DESPESA,
        tipoSemTransferencia = null,
        estabelecimento = "Mercado",
        estabelecimentoNormalizado = "MERCADO",
        beneficio = BeneficioVoucher.VA,
        saldoObservadoCentavos = 45000L,
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.MANUAL,
        instituicaoOrigem = InstituicaoFinanceira.CAJU,
        confianca = 1.0,
        motivoPendencia = null,
        hashDuplicata = null,
        observacao = null,
        parcelaNumero = 1,
        totalParcelas = 1,
        valorTotalCompraCentavos = null,
        criadoEm = instante,
        atualizadoEm = instante,
        excluidoEm = null,
        textoBruto = null,
    )

    private companion object {
        val INSTANTE: Instant = Instant.parse("2026-11-10T12:00:00Z")
    }
}

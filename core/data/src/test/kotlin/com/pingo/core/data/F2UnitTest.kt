package com.pingo.core.data

import com.pingo.core.data.captura.FakeContas
import com.pingo.core.data.captura.FakeInstrumentos
import com.pingo.core.data.captura.FakeLancamentos
import com.pingo.core.data.captura.MutableClock
import com.pingo.core.data.categoria.FakeCategoriaDao
import com.pingo.core.data.impl.FakeContaDao
import com.pingo.core.data.impl.FakeInstrumentoDao
import com.pingo.core.data.impl.FontesContasProjection
import com.pingo.core.data.impl.InicioFinanceiroProjectionImpl
import com.pingo.core.data.revisao.ConfirmarPendencias
import com.pingo.core.data.voucher.MaterializarRecargasVencidasUseCase
import com.pingo.core.database.dao.ConfiguracaoCartaoDao
import com.pingo.core.database.dao.FaturaDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.dao.RecargaProgramadaVoucherDao
import com.pingo.core.database.dao.ReferenciaSaldoVoucherDao
import com.pingo.core.database.entity.ConfiguracaoCartaoEntity
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.FaturaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import com.pingo.core.database.entity.ReferenciaSaldoVoucherEntity
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.organizacao.EstadoBlocoFinanceiro
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class F2UnitTest {
    private val agora: Instant = Instant.parse("2026-11-10T15:00:00Z")
    private val clock = MutableClock(agora, ZoneId.of("America/Sao_Paulo"))

    @Test
    fun `confirmar pendencias nao aprova divergencia ou beneficio ambiguo em lote`() = runTest {
        val contas = FakeContas()
        val instrumentos = FakeInstrumentos(contas)
        val lancamentos = FakeLancamentos(instrumentos)
        val confirmar = ConfirmarPendencias(lancamentos, instrumentos, clock)

        val inst = instrumentos.criar(InstituicaoFinanceira.CAJU)
        val divergente = pendenteBase(inst.publicId, "lanc-div", MotivoPendencia.DIVERGENCIA_RECARGA.name)
        val ambiguo = pendenteBase(inst.publicId, "lanc-amb", MotivoPendencia.BENEFICIO_AMBIGUO.name)
        val normal = pendenteBase(inst.publicId, "lanc-ok", MotivoPendencia.BAIXA_CONFIANCA.name)
            .copy(tipo = TipoLancamento.DESPESA)
        lancamentos.salvarTodos(listOf(divergente, ambiguo, normal))

        val resultado = confirmar.confirmarTodos()

        assertEquals(1, resultado.confirmados)
        assertEquals(2, resultado.mantidos)
        assertEquals(StatusLancamento.PENDENTE, lancamentos.buscar("lanc-div")?.status)
        assertEquals(StatusLancamento.PENDENTE, lancamentos.buscar("lanc-amb")?.status)
        assertEquals(StatusLancamento.CONFIRMADO, lancamentos.buscar("lanc-ok")?.status)
    }

    @Test
    fun `mesma instancia no inicio isola falha de faturas sem afetar vouchers`() = runTest {
        val fontes = fontesComContasEInstrumentos(
            faturaDao = FaturaDaoFalhando(),
        )
        val materializador = materializadorDe(fontes)
        val inicio = InicioFinanceiroProjectionImpl(fontes, materializador, clock)

        val estadosFaturas = inicio.observeFaturasAbertas().take(2).toList()
        val estadosVouchers = inicio.observeSaldosVoucher().take(2).toList()

        assertEquals(EstadoBlocoFinanceiro.Erro, estadosFaturas.last())
        val sucesso = estadosVouchers.last() as EstadoBlocoFinanceiro.Sucesso
        assertEquals(42000L, sucesso.dados.single().saldoCentavos)
    }

    @Test
    fun `mesma instancia no inicio isola falha de vouchers sem afetar faturas`() = runTest {
        val fontes = fontesComContasEInstrumentos(
            referenciaDao = ReferenciaDaoFalhando(),
        )
        val materializador = materializadorDe(fontes)
        val inicio = InicioFinanceiroProjectionImpl(fontes, materializador, clock)

        val estadosFaturas = inicio.observeFaturasAbertas().take(2).toList()
        val estadosVouchers = inicio.observeSaldosVoucher().take(2).toList()

        assertEquals(EstadoBlocoFinanceiro.Erro, estadosVouchers.last())
        val sucesso = estadosFaturas.last() as EstadoBlocoFinanceiro.Sucesso
        assertEquals("inst-cartao", sucesso.dados.single().instrumentoPublicId)
    }

    private fun materializadorDe(fontes: FontesContasProjection): MaterializarRecargasVencidasUseCase = MaterializarRecargasVencidasUseCase(
        transacaoLocal = TransacaoLocal.Direta,
        instrumentoDao = fontes.instrumentoDao,
        recargaDao = fontes.recargaDao,
        lancamentoDao = fontes.lancamentoDao,
        categoriaDao = FakeCategoriaDao(),
        clock = clock,
    )

    private fun fontesComContasEInstrumentos(
        faturaDao: FaturaDao = FakeFaturaDaoVazio(),
        referenciaDao: ReferenciaSaldoVoucherDao = FakeReferenciaDaoVazio(),
    ): FontesContasProjection {
        val contaDao = FakeContaDao()
        val instDao = FakeInstrumentoDao()
        runBlocking { semearContasEInstrumentos(contaDao, instDao) }
        return FontesContasProjection(
            contaDao = contaDao,
            instrumentoDao = instDao,
            faturaDao = faturaDao,
            configuracaoCartaoDao = FakeConfiguracaoCartaoDaoVazio(),
            referenciaDao = referenciaDao,
            recargaDao = FakeRecargaDaoVazio(),
            lancamentoDao = FakeLancamentoDaoVazio(),
        )
    }

    private suspend fun semearContasEInstrumentos(contaDao: FakeContaDao, instDao: FakeInstrumentoDao) {
        contaDao.upsert(ContaEntity("c-1", InstituicaoFinanceira.NUBANK, "Nu", TipoConta.CORRENTE, false, agora, agora, null))
        contaDao.upsert(ContaEntity("c-2", InstituicaoFinanceira.CAJU, "Caju", TipoConta.BENEFICIO, false, agora, agora, null))
        instDao.upsertAll(listOf(cartaoTeste(), voucherTeste()))
    }

    private fun cartaoTeste(): InstrumentoEntity = InstrumentoEntity(
        publicId = "inst-cartao",
        contaPublicId = "c-1",
        tipo = TipoInstrumento.CREDITO,
        apelido = "Cartao",
        ultimosDigitos = "1234",
        arquivado = false,
        arquivadoPorCascata = false,
        fechamentoDia = 15,
        vencimentoDia = 22,
        beneficio = null,
        saldoCentavos = null,
        diaRecarga = null,
        valorRecargaCentavos = null,
        limiteCreditoCentavos = null,
        criadoEm = agora,
        atualizadoEm = agora,
        excluidoEm = null,
    )

    private fun voucherTeste(): InstrumentoEntity = InstrumentoEntity(
        publicId = "inst-va",
        contaPublicId = "c-2",
        tipo = TipoInstrumento.VOUCHER,
        apelido = "VA",
        ultimosDigitos = "1234",
        arquivado = false,
        arquivadoPorCascata = false,
        fechamentoDia = null,
        vencimentoDia = null,
        beneficio = BeneficioVoucher.VA,
        saldoCentavos = 42000L,
        diaRecarga = null,
        valorRecargaCentavos = null,
        limiteCreditoCentavos = null,
        criadoEm = agora,
        atualizadoEm = agora,
        excluidoEm = null,
    )

    private fun pendenteBase(instId: String, id: String, motivo: String) = Lancamento(
        publicId = id,
        instrumentoPublicId = instId,
        valorCentavos = 50000L,
        dataHora = agora,
        tipo = TipoLancamento.RECEITA,
        estabelecimento = "Recarga Caju",
        status = StatusLancamento.PENDENTE,
        origem = OrigemLancamento.NOTIFICACAO,
        confianca = 1.0,
        motivoPendencia = motivo,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private open class FakeFaturaDaoVazio : FaturaDao {
        override fun observeAtivas(): Flow<List<FaturaEntity>> = flowOf(emptyList())

        override suspend fun listarAtivas(): List<FaturaEntity> = emptyList()

        override suspend fun buscar(publicId: String): FaturaEntity? = null

        override fun observePorInstrumento(instrumentoPublicId: String): Flow<List<FaturaEntity>> = flowOf(emptyList())

        override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<FaturaEntity> = emptyList()

        override suspend fun buscarPorInstrumentoECompetencia(instrumentoPublicId: String, competencia: LocalDate): FaturaEntity? = null

        override suspend fun atualizadasDesde(desde: Instant): List<FaturaEntity> = emptyList()

        override suspend fun upsert(fatura: FaturaEntity) = Unit

        override suspend fun upsertAll(faturas: List<FaturaEntity>) = Unit

        override suspend fun marcarExcluida(publicId: String, quando: Instant) = Unit

        override suspend fun repointarPagamento(de: String, para: String) = Unit
    }

    private class FaturaDaoFalhando : FakeFaturaDaoVazio() {
        override fun observeAtivas(): Flow<List<FaturaEntity>> = flow {
            throw IllegalStateException("falha no bloco de faturas")
        }
    }

    private class FakeConfiguracaoCartaoDaoVazio : ConfiguracaoCartaoDao {
        override fun observeAtivas(): Flow<List<ConfiguracaoCartaoEntity>> = flowOf(emptyList())

        override fun observePorInstrumento(instrumentoPublicId: String): Flow<List<ConfiguracaoCartaoEntity>> = flowOf(emptyList())

        override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<ConfiguracaoCartaoEntity> = emptyList()

        override suspend fun listarAtivas(): List<ConfiguracaoCartaoEntity> = emptyList()

        override suspend fun buscar(publicId: String): ConfiguracaoCartaoEntity? = null

        override suspend fun atualizadasDesde(desde: Instant): List<ConfiguracaoCartaoEntity> = emptyList()

        override suspend fun upsert(configuracao: ConfiguracaoCartaoEntity) = Unit

        override suspend fun upsertAll(configuracoes: List<ConfiguracaoCartaoEntity>) = Unit

        override suspend fun marcarExcluida(publicId: String, quando: Instant) = Unit
    }

    private open class FakeReferenciaDaoVazio : ReferenciaSaldoVoucherDao {
        override fun observeAtivas(): Flow<List<ReferenciaSaldoVoucherEntity>> = flowOf(emptyList())

        override fun observePorInstrumento(instrumentoPublicId: String): Flow<List<ReferenciaSaldoVoucherEntity>> = flowOf(emptyList())

        override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<ReferenciaSaldoVoucherEntity> = emptyList()

        override suspend fun buscar(publicId: String): ReferenciaSaldoVoucherEntity? = null

        override suspend fun buscarPorLancamento(lancamentoPublicId: String): ReferenciaSaldoVoucherEntity? = null

        override suspend fun buscarPorLancamentoIncluindoExcluidas(lancamentoPublicId: String): ReferenciaSaldoVoucherEntity? = null

        override suspend fun atualizadasDesde(desde: Instant): List<ReferenciaSaldoVoucherEntity> = emptyList()

        override suspend fun upsert(referencia: ReferenciaSaldoVoucherEntity) = Unit

        override suspend fun upsertAll(referencias: List<ReferenciaSaldoVoucherEntity>) = Unit

        override suspend fun repointarLancamento(de: String, para: String) = Unit

        override suspend fun marcarExcluida(publicId: String, quando: Instant) = Unit
    }

    private class ReferenciaDaoFalhando : FakeReferenciaDaoVazio() {
        override fun observeAtivas(): Flow<List<ReferenciaSaldoVoucherEntity>> = flow {
            throw IllegalStateException("falha no bloco de vouchers")
        }
    }

    private class FakeRecargaDaoVazio : RecargaProgramadaVoucherDao {
        override fun observeAtivas(): Flow<List<RecargaProgramadaVoucherEntity>> = flowOf(emptyList())

        override fun observePorInstrumento(instrumentoPublicId: String): Flow<List<RecargaProgramadaVoucherEntity>> = flowOf(emptyList())

        override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<RecargaProgramadaVoucherEntity> = emptyList()

        override suspend fun listarAtivas(): List<RecargaProgramadaVoucherEntity> = emptyList()

        override suspend fun listarPendentesAtivas(): List<RecargaProgramadaVoucherEntity> = emptyList()

        override suspend fun buscarPorCompetencia(instrumentoPublicId: String, competenciaMes: LocalDate): RecargaProgramadaVoucherEntity? =
            null

        override suspend fun buscarPorLancamento(lancamentoPublicId: String): List<RecargaProgramadaVoucherEntity> = emptyList()

        override suspend fun buscar(publicId: String): RecargaProgramadaVoucherEntity? = null

        override suspend fun atualizadasDesde(desde: Instant): List<RecargaProgramadaVoucherEntity> = emptyList()

        override suspend fun upsert(recarga: RecargaProgramadaVoucherEntity) = Unit

        override suspend fun upsertAll(recargas: List<RecargaProgramadaVoucherEntity>) = Unit

        override suspend fun repointarLancamento(de: String, para: String) = Unit

        override suspend fun marcarExcluida(publicId: String, quando: Instant) = Unit
    }

    private class FakeLancamentoDaoVazio : LancamentoDao() {
        override fun observeAtivos(): Flow<List<LancamentoEntity>> = flowOf(emptyList())

        override fun observePorPeriodo(inicio: Instant, fim: Instant): Flow<List<LancamentoEntity>> = flowOf(emptyList())

        override fun observeElegiveisParaRelatorioNoPeriodo(inicioInclusivo: Instant, fimExclusivo: Instant): Flow<List<LancamentoEntity>> =
            flowOf(emptyList())

        override suspend fun listarElegiveisParaRelatorioNoPeriodo(
            inicioInclusivo: Instant,
            fimExclusivo: Instant,
        ): List<LancamentoEntity> = emptyList()

        override fun observeElegiveisParaDeteccao(): Flow<List<LancamentoEntity>> = flowOf(emptyList())

        override suspend fun listarElegiveisParaDeteccao(): List<LancamentoEntity> = emptyList()

        override suspend fun listarPorFatura(faturaPublicId: String): List<LancamentoEntity> = emptyList()

        override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<LancamentoEntity> = emptyList()

        override fun observePorAssinatura(assinaturaPublicId: String): Flow<List<LancamentoEntity>> = flowOf(emptyList())

        override suspend fun listarPorAssinatura(assinaturaPublicId: String): List<LancamentoEntity> = emptyList()

        override suspend fun buscar(publicId: String): LancamentoEntity? = null

        override suspend fun buscarPorHash(hash: String): LancamentoEntity? = null

        override suspend fun ultimoInstrumentoUsado(instrumentos: List<String>): String? = null

        override suspend fun capturadosSemelhantes(
            instrumentoPublicId: String,
            valorCentavos: Long,
            inicio: Instant,
            fim: Instant,
            origem: OrigemLancamento,
        ): List<LancamentoEntity> = emptyList()

        override suspend fun candidatosDeTransferencia(
            tipo: TipoLancamento,
            valorCentavos: Long,
            inicio: Instant,
            fim: Instant,
            contaPublicId: String,
            origem: OrigemLancamento,
        ): List<LancamentoEntity> = emptyList()

        override suspend fun atualizadosDesde(desde: Instant): List<LancamentoEntity> = emptyList()

        override suspend fun upsert(lancamento: LancamentoEntity) = Unit

        override suspend fun upsertAll(lancamentos: List<LancamentoEntity>) = Unit

        override suspend fun marcarExcluido(publicId: String, quando: Instant) = Unit

        override suspend fun definirTextoBruto(publicId: String, textoBruto: String) = Unit

        override suspend fun repointarCompraPai(de: String, para: String) = Unit

        override suspend fun repointarEstorno(de: String, para: String) = Unit

        override suspend fun apagar(publicId: String) = Unit
    }
}

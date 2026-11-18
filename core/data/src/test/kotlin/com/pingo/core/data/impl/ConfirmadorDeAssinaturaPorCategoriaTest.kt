package com.pingo.core.data.impl

import com.pingo.core.data.assinatura.AgendadorDeAvisosDeAssinatura
import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.database.dao.AssinaturaDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.entity.AssinaturaEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.model.Categoria
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional

class ConfirmadorDeAssinaturaPorCategoriaTest {
    private val agora: Instant = Instant.parse("2026-11-18T12:00:00Z")
    private val clock: Clock = Clock.fixed(agora, ZoneOffset.UTC)
    private val catAssinaturas = Categoria("cat-ass", "Assinaturas", TipoCategoria.DESPESA, true, false, agora, agora)
    private val catCompras = Categoria("cat-comp", "Compras", TipoCategoria.DESPESA, true, false, agora, agora)

    @Test
    fun `salvar na categoria assinaturas cria assinatura confirmada e vincula lancamento`() = runBlocking {
        val assinaturaDao = FakeAssinaturaDao()
        val lancamentoDao = FakeLancamentoDaoAssinatura()
        val confirmador = ConfirmadorDeAssinaturaPorCategoria(
            assinaturaDao = assinaturaDao,
            lancamentoDao = lancamentoDao,
            agendadorDeAvisos = AgendadorDeAvisosOpcional(),
            clock = clock,
        )
        val base = lancamentoBase("l-1", "Netflix", 4490L, catAssinaturas.publicId)

        val vinculado = confirmador.prepararAoSalvarManual(base, catAssinaturas)

        assertNotNull(vinculado.assinaturaPublicId)
        val entidade = assinaturaDao.buscar(vinculado.assinaturaPublicId!!)
        assertNotNull(entidade)
        assertEquals(StatusAssinatura.CONFIRMADA, entidade?.status)
        assertEquals("netflix", entidade?.estabelecimentoNormalizado)
        assertEquals(4490L, entidade?.valorReferenciaCentavos)
        assertEquals(LocalDate.of(2026, 12, 18), entidade?.proximaCobrancaEstimada)
    }

    @Test
    fun `trocar categoria para nao assinatura remove vinculo e exclui assinatura orfa`() = runBlocking {
        val assinaturaDao = FakeAssinaturaDao()
        val lancamentoDao = FakeLancamentoDaoAssinatura()
        val agendador = FakeAgendador()
        val confirmador = ConfirmadorDeAssinaturaPorCategoria(
            assinaturaDao = assinaturaDao,
            lancamentoDao = lancamentoDao,
            agendadorDeAvisos = AgendadorDeAvisosOpcional(Optional.of(agendador)),
            clock = clock,
        )
        val base = lancamentoBase("l-1", "Spotify", 2190L, catAssinaturas.publicId)
        val vinculado = confirmador.prepararAoSalvarManual(base, catAssinaturas)
        val assinaturaId = requireNotNull(vinculado.assinaturaPublicId)

        val editado = confirmador.aoEditarLancamento(
            original = vinculado,
            atualizado = vinculado.copy(categoriaPublicId = catCompras.publicId),
            novaCategoria = catCompras,
        )

        assertNull(editado.assinaturaPublicId)
        assertNotNull(assinaturaDao.buscar(assinaturaId)?.excluidoEm)
        assertEquals(listOf(assinaturaId), agendador.canceladas)
    }

    @Test
    fun `nao reativa assinatura ja cancelada ao salvar na categoria assinaturas`() = runBlocking {
        val assinaturaDao = FakeAssinaturaDao()
        assinaturaDao.upsert(
            AssinaturaEntity(
                publicId = "ass-canc",
                estabelecimento = "Prime Video",
                estabelecimentoNormalizado = "prime video",
                valorReferenciaCentavos = 1990L,
                ultimaCobranca = LocalDate.of(2026, 10, 10),
                proximaCobrancaEstimada = LocalDate.of(2026, 11, 10),
                status = StatusAssinatura.CANCELADA,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
                instrumentoPublicId = "inst-1",
            ),
        )
        val confirmador = ConfirmadorDeAssinaturaPorCategoria(
            assinaturaDao = assinaturaDao,
            lancamentoDao = FakeLancamentoDaoAssinatura(),
            agendadorDeAvisos = AgendadorDeAvisosOpcional(),
            clock = clock,
        )
        val base = lancamentoBase("l-2", "Prime Video", 1990L, catAssinaturas.publicId)

        val resultado = confirmador.prepararAoSalvarManual(base, catAssinaturas)

        assertNull(resultado.assinaturaPublicId)
        assertEquals(StatusAssinatura.CANCELADA, assinaturaDao.buscar("ass-canc")?.status)
    }

    private fun lancamentoBase(publicId: String, estabelecimento: String, valor: Long, categoriaId: String): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = "inst-1",
        categoriaPublicId = categoriaId,
        valorCentavos = valor,
        dataHora = agora,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = estabelecimento,
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.MANUAL,
        confianca = 1.0,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private class FakeAgendador : AgendadorDeAvisosDeAssinatura {
        val canceladas = mutableListOf<String>()

        override suspend fun reconciliarTodas() = Unit

        override suspend fun cancelarParaAssinatura(assinaturaPublicId: String) {
            canceladas += assinaturaPublicId
        }

        override suspend fun cancelarTodos() = Unit
    }

    private class FakeAssinaturaDao : AssinaturaDao {
        private val itens = MutableStateFlow<List<AssinaturaEntity>>(emptyList())

        override fun observeAtivas(): Flow<List<AssinaturaEntity>> = itens

        override fun observePorStatus(status: StatusAssinatura): Flow<List<AssinaturaEntity>> = itens

        override suspend fun listarAtivas(): List<AssinaturaEntity> = itens.value.filter { it.excluidoEm == null }

        override suspend fun buscar(publicId: String): AssinaturaEntity? = itens.value.firstOrNull { it.publicId == publicId }

        override suspend fun buscarPorChaveComInstrumento(
            instrumentoPublicId: String,
            estabelecimentoNormalizado: String,
        ): AssinaturaEntity? = itens.value.firstOrNull {
            it.excluidoEm == null &&
                it.instrumentoPublicId == instrumentoPublicId &&
                it.estabelecimentoNormalizado == estabelecimentoNormalizado
        }

        override suspend fun buscarLegadaPorEstabelecimento(estabelecimentoNormalizado: String): AssinaturaEntity? = null

        override suspend fun atualizadasDesde(desde: Instant): List<AssinaturaEntity> = itens.value

        override suspend fun upsert(assinatura: AssinaturaEntity) {
            itens.value = itens.value.filterNot { it.publicId == assinatura.publicId } + assinatura
        }

        override suspend fun upsertAll(assinaturas: List<AssinaturaEntity>) {
            assinaturas.forEach { upsert(it) }
        }

        override suspend fun marcarExcluida(publicId: String, quando: Instant) {
            itens.value = itens.value.map { if (it.publicId == publicId) it.copy(excluidoEm = quando) else it }
        }
    }

    @Suppress("TooManyFunctions")
    private class FakeLancamentoDaoAssinatura : LancamentoDao() {
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

        override suspend fun listarParcelasDaCompra(raizPublicId: String): List<LancamentoEntity> = emptyList()

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

        override suspend fun marcarExcluidos(publicIds: List<String>, quando: Instant) = Unit

        override suspend fun definirTextoBruto(publicId: String, textoBruto: String) = Unit

        override suspend fun repointarCompraPai(de: String, para: String) = Unit

        override suspend fun repointarEstorno(de: String, para: String) = Unit

        override suspend fun apagar(publicId: String) = Unit
    }
}

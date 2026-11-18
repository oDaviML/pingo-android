package com.pingo.core.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.data.impl.GestaoDeCategoriasRepositoryImpl
import com.pingo.core.data.impl.RegraCategoriaRepositoryImpl
import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.database.entity.RegraCategoriaEntity
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class GestaoDeCategoriasRoomTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var ambiente: BancoRoomDeTeste
    private lateinit var gestao: GestaoDeCategoriasRepositoryImpl

    @Before
    fun setUp() {
        ambiente = BancoRoomDeTeste(context, "gestao-categorias-test.db")
        gestao = GestaoDeCategoriasRepositoryImpl(
            categoriaRepository = CategoriaRepositoryImpl(ambiente.database.categoriaDao()),
            regraCategoriaRepository = RegraCategoriaRepositoryImpl(ambiente.database.regraCategoriaDao()),
            gestaoDeCategoriasDao = ambiente.database.gestaoDeCategoriasDao(),
            clock = ambiente.clock,
        )
    }

    @After
    fun tearDown() {
        ambiente.fechar()
    }

    @Test
    fun contaLancamentosERegrasPorCategoriaERegraAprendida() = runBlocking {
        inserirBase(estabelecimento = "Padaria do Zé", regraEstabelecimento = "padaria do zé")

        val categorias = gestao.observeCategoriasComContagem().first()
        val regras = gestao.observeRegrasComUso().first()

        assertEquals(1, categorias.first { it.categoria.publicId == "cat-lazer" }.quantidadeLancamentos)
        assertEquals(1, categorias.first { it.categoria.publicId == "cat-lazer" }.quantidadeRegras)
        assertEquals(1, regras.single { it.regra.publicId == "regra-1" }.quantidadeLancamentos)
    }

    @Test
    fun excluirComLancamentosExigeDestinoValidoEMigraAtomicamenteSemOrfaos() = runBlocking {
        inserirBase(estabelecimento = "Cinema", regraEstabelecimento = "cinema")

        assertEquals(ResultadoExclusaoCategoria.DestinoObrigatorio, gestao.excluirCategoria("cat-lazer"))
        assertEquals(ResultadoExclusaoCategoria.DestinoInvalido, gestao.excluirCategoria("cat-lazer", "cat-salario"))
        assertEquals(ResultadoExclusaoCategoria.DestinoInvalido, gestao.excluirCategoria("cat-lazer", "cat-arquivada"))

        val resultado = gestao.excluirCategoria("cat-lazer", "cat-alimentacao")
        val lancamentoMigrado = ambiente.database.lancamentoDao().buscar("l1")
        val regraMigrada = ambiente.database.regraCategoriaDao().buscar("regra-1")

        assertEquals(ResultadoExclusaoCategoria.Excluida, resultado)
        assertEquals("cat-alimentacao", lancamentoMigrado?.categoriaPublicId)
        assertEquals("cat-alimentacao", regraMigrada?.categoriaPublicId)
        assertNull(ambiente.database.categoriaDao().listarAtivas().firstOrNull { it.publicId == "cat-lazer" })
    }

    @Test
    fun excluirCategoriaSemUsoEExcluirRegraFuncionamLimpos() = runBlocking {
        inserirCategoriasBase()
        ambiente.database.regraCategoriaDao().upsert(
            RegraCategoriaEntity("regra-2", "cat-alimentacao", "cafeteria", ambiente.agora, ambiente.agora, null),
        )

        val excluida = gestao.excluirCategoria("cat-lazer")
        gestao.excluirRegra("regra-2")

        assertEquals(ResultadoExclusaoCategoria.Excluida, excluida)
        assertNull(ambiente.database.regraCategoriaDao().buscarPorEstabelecimento("cafeteria"))
    }

    private suspend fun inserirBase(estabelecimento: String, regraEstabelecimento: String) {
        inserirCategoriasBase()
        val t = ambiente.agora
        ambiente.database.contaDao().upsert(
            ContaEntity("conta-1", InstituicaoFinanceira.ITAU, "Itaú", TipoConta.CORRENTE, false, t, t, null),
        )
        ambiente.database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = "inst-1",
                contaPublicId = "conta-1",
                tipo = TipoInstrumento.CREDITO,
                apelido = "Cartão",
                ultimosDigitos = "1234",
                arquivado = false,
                fechamentoDia = null,
                vencimentoDia = null,
                beneficio = null,
                saldoCentavos = null,
                diaRecarga = null,
                valorRecargaCentavos = null,
                limiteCreditoCentavos = null,
                criadoEm = t,
                atualizadoEm = t,
                excluidoEm = null,
            ),
        )
        ambiente.database.regraCategoriaDao().upsert(
            RegraCategoriaEntity("regra-1", "cat-lazer", regraEstabelecimento, t, t, null),
        )
        ambiente.database.lancamentoDao().upsert(
            LancamentoEntity(
                publicId = "l1",
                instrumentoPublicId = "inst-1",
                categoriaPublicId = "cat-lazer",
                faturaPublicId = null,
                assinaturaPublicId = null,
                compraPaiPublicId = null,
                estornoDePublicId = null,
                transferenciaId = null,
                valorCentavos = 2500L,
                dataHora = t,
                tipo = TipoLancamento.DESPESA,
                estabelecimento = estabelecimento,
                status = StatusLancamento.CONFIRMADO,
                origem = OrigemLancamento.NOTIFICACAO,
                confianca = 0.9,
                motivoPendencia = null,
                hashDuplicata = "hash-1",
                observacao = null,
                parcelaNumero = 1,
                totalParcelas = 1,
                valorTotalCompraCentavos = null,
                criadoEm = t,
                atualizadoEm = t,
                excluidoEm = null,
                textoBruto = null,
            ),
        )
    }

    private suspend fun inserirCategoriasBase() {
        val t = Instant.parse("2026-11-10T10:00:00Z")
        ambiente.database.categoriaDao().upsertAll(
            listOf(
                CategoriaEntity("cat-lazer", "Lazer", TipoCategoria.DESPESA, false, false, t, t, null),
                CategoriaEntity("cat-alimentacao", "Alimentação", TipoCategoria.DESPESA, true, false, t, t, null),
                CategoriaEntity("cat-arquivada", "Arquivada", TipoCategoria.DESPESA, false, true, t, t, null),
                CategoriaEntity("cat-salario", "Salário", TipoCategoria.RECEITA, true, false, t, t, null),
            ),
        )
    }
}

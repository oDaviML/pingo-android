package com.pingo.feature.settings.impl

import com.pingo.core.data.CategoriaComContagem
import com.pingo.core.data.GestaoDeCategoriasRepository
import com.pingo.core.data.RegraCategoriaComUso
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.ResultadoExclusaoCategoria
import com.pingo.core.model.Categoria
import com.pingo.core.model.RegraCategoria
import com.pingo.core.model.TipoCategoria
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriasERegrasViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val agora = Instant.parse("2026-11-18T12:00:00Z")
    private val catLazer = Categoria("cat-1", "Lazer", TipoCategoria.DESPESA, criadoEm = agora, atualizadoEm = agora)
    private val catMercado = Categoria("cat-2", "Mercado", TipoCategoria.DESPESA, padrao = true, criadoEm = agora, atualizadoEm = agora)
    private val catSalario = Categoria("cat-3", "Salário", TipoCategoria.RECEITA, padrao = true, criadoEm = agora, atualizadoEm = agora)
    private val fakeRepo = FakeGestaoRepository(
        iniciais = listOf(
            CategoriaComContagem(catLazer, quantidadeLancamentos = 3, quantidadeRegras = 1),
            CategoriaComContagem(catMercado, quantidadeLancamentos = 0, quantidadeRegras = 0),
            CategoriaComContagem(catSalario, quantidadeLancamentos = 1, quantidadeRegras = 0),
        ),
        regrasIniciais = listOf(
            RegraCategoriaComUso(
                regra = RegraCategoria("r1", "cat-1", "cinema central", agora, agora),
                nomeCategoria = "Lazer",
                quantidadeLancamentos = 3,
            ),
        ),
    )

    @Test
    fun `criar categoria valida nome vazio e duplicado`() = runTest(UnconfinedTestDispatcher()) {
        val vm = CategoriasERegrasViewModel(fakeRepo)

        vm.abrirDialogoCriar()
        vm.confirmarCriarCategoria()
        assertNotNull(vm.uiState.value.dialogoCriar?.erro)

        vm.atualizarNovaCategoria(nome = "Lazer")
        vm.confirmarCriarCategoria()
        assertNotNull(vm.uiState.value.dialogoCriar?.erro)

        vm.atualizarNovaCategoria(nome = "Pets", tipo = TipoCategoria.DESPESA)
        vm.confirmarCriarCategoria()
        assertNull(vm.uiState.value.dialogoCriar)
    }

    @Test
    fun `excluir categoria com lancamentos filtra destinos do mesmo tipo e exige selecao`() = runTest(UnconfinedTestDispatcher()) {
        val vm = CategoriasERegrasViewModel(fakeRepo)
        val lazerItem = vm.uiState.value.categorias.first { it.categoria.publicId == "cat-1" }

        vm.abrirDialogoExcluirCategoria(lazerItem)
        val dialogo = vm.uiState.value.dialogoExcluirCategoria!!
        assertEquals(listOf("cat-2"), dialogo.destinosDisponiveis.map { it.publicId })

        vm.confirmarExcluirCategoria()
        assertNotNull(vm.uiState.value.dialogoExcluirCategoria?.erro)

        vm.selecionarDestinoExclusao("cat-2")
        vm.confirmarExcluirCategoria()
        assertNull(vm.uiState.value.dialogoExcluirCategoria)
        assertEquals("cat-2", fakeRepo.ultimoDestinoMigracao)
    }

    @Test
    fun `alternar arquivo e excluir regra aprendida delegam ao repositorio`() = runTest(UnconfinedTestDispatcher()) {
        val vm = CategoriasERegrasViewModel(fakeRepo)
        val lazerItem = vm.uiState.value.categorias.first { it.categoria.publicId == "cat-1" }
        val regraItem = vm.uiState.value.regras.single()

        vm.alternarArquivo(lazerItem)
        assertTrue(fakeRepo.alternouArquivo)

        vm.solicitarExcluirRegra(regraItem)
        vm.confirmarExcluirRegra()
        assertEquals("r1", fakeRepo.regraExcluidaId)
        assertNull(vm.uiState.value.dialogoExcluirRegra)
    }

    private class FakeGestaoRepository(iniciais: List<CategoriaComContagem>, regrasIniciais: List<RegraCategoriaComUso>) :
        GestaoDeCategoriasRepository {
        private val categorias = MutableStateFlow(iniciais)
        private val regras = MutableStateFlow(regrasIniciais)
        var ultimoDestinoMigracao: String? = null
        var alternouArquivo = false
        var regraExcluidaId: String? = null

        override fun observeCategoriasComContagem(): Flow<List<CategoriaComContagem>> = categorias

        override fun observeRegrasComUso(): Flow<List<RegraCategoriaComUso>> = regras

        override suspend fun criarCategoria(nome: String, tipo: TipoCategoria): ResultadoCategoria {
            val limpo = nome.trim()
            return when {
                limpo.isEmpty() -> ResultadoCategoria.NomeVazio
                categorias.value.any { it.categoria.nome.equals(limpo, ignoreCase = true) && it.categoria.tipo == tipo } ->
                    ResultadoCategoria.NomeEmUso
                else -> ResultadoCategoria.Sucesso(
                    Categoria("nova", limpo, tipo, criadoEm = Instant.EPOCH, atualizadoEm = Instant.EPOCH),
                )
            }
        }

        override suspend fun renomearCategoria(publicId: String, novoNome: String): ResultadoCategoria = ResultadoCategoria.NaoEncontrada

        override suspend fun alternarArquivoCategoria(publicId: String, arquivada: Boolean) {
            alternouArquivo = true
        }

        override suspend fun excluirCategoria(publicId: String, categoriaDestinoPublicId: String?): ResultadoExclusaoCategoria {
            val item = categorias.value.firstOrNull { it.categoria.publicId == publicId } ?: return ResultadoExclusaoCategoria.NaoEncontrada
            if (item.quantidadeLancamentos > 0 && categoriaDestinoPublicId == null) {
                return ResultadoExclusaoCategoria.DestinoObrigatorio
            }
            ultimoDestinoMigracao = categoriaDestinoPublicId
            return ResultadoExclusaoCategoria.Excluida
        }

        override suspend fun excluirRegra(publicId: String) {
            regraExcluidaId = publicId
        }
    }
}

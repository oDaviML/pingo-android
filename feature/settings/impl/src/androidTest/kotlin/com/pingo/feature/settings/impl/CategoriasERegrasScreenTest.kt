package com.pingo.feature.settings.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.CategoriaComContagem
import com.pingo.core.data.RegraCategoriaComUso
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.Categoria
import com.pingo.core.model.RegraCategoria
import com.pingo.core.model.TipoCategoria
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CategoriasERegrasScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val agora = Instant.parse("2026-11-18T12:00:00Z")

    @Test
    fun exibeAbasCategoriasERegrasEDisparaAcoes() {
        var abaSelecionada = 0
        var abriuCriar = 0
        val cat = Categoria("cat-1", "Lazer", TipoCategoria.DESPESA, criadoEm = agora, atualizadoEm = agora)
        val regra = RegraCategoria("r1", "cat-1", "padaria central", agora, agora)

        composeRule.setContent {
            PingoTheme {
                CategoriasERegrasContent(
                    state = CategoriasERegrasUiState(
                        abaSelecionada = 0,
                        categorias = listOf(CategoriaComContagem(cat, 2, 1)),
                        regras = listOf(RegraCategoriaComUso(regra, "Lazer", 2)),
                    ),
                    onBack = {},
                    onSelecionarAba = { abaSelecionada = it },
                    onAbrirCriar = { abriuCriar++ },
                    onAbrirRenomear = {},
                    onAlternarArquivo = {},
                    onAbrirExcluirCategoria = {},
                    onSolicitarExcluirRegra = {},
                )
            }
        }

        composeRule.onNodeWithText("Lazer").assertIsDisplayed()
        composeRule.onNodeWithText("Despesa • 2 lançamentos • 1 regras").assertIsDisplayed()
        composeRule.onNodeWithTag("botao_nova_categoria").performClick()
        composeRule.onNodeWithText("Regras aprendidas").performClick()

        assertEquals(1, abriuCriar)
        assertEquals(1, abaSelecionada)
    }

    @Test
    fun dialogoExclusaoComLancamentosExigeEscolhaDeDestino() {
        val origem = Categoria("cat-1", "Lazer", TipoCategoria.DESPESA, criadoEm = agora, atualizadoEm = agora)
        val destino = Categoria("cat-2", "Outros", TipoCategoria.DESPESA, padrao = true, criadoEm = agora, atualizadoEm = agora)

        composeRule.setContent {
            PingoTheme {
                CategoriasERegrasContent(
                    state = CategoriasERegrasUiState(
                        dialogoExcluirCategoria = DialogoExcluirCategoria(
                            item = CategoriaComContagem(origem, 3, 0),
                            destinosDisponiveis = listOf(destino),
                            destinoSelecionadoId = null,
                        ),
                    ),
                    onBack = {},
                    onSelecionarAba = {},
                    onAbrirCriar = {},
                    onAbrirRenomear = {},
                    onAlternarArquivo = {},
                    onAbrirExcluirCategoria = {},
                    onSolicitarExcluirRegra = {},
                )
            }
        }

        composeRule.onNodeWithTag("botao_confirmar_exclusao_migracao").assertDoesNotExist()
    }

    @Test
    fun exibeSheetDeMigracaoEDesabilitaBotaoSemDestino() {
        val origem = Categoria("cat-1", "Lazer", TipoCategoria.DESPESA, criadoEm = agora, atualizadoEm = agora)
        val destino = Categoria("cat-2", "Outros", TipoCategoria.DESPESA, padrao = true, criadoEm = agora, atualizadoEm = agora)
        val vm = CategoriasERegrasViewModel(
            repository = object : com.pingo.core.data.GestaoDeCategoriasRepository {
                override fun observeCategoriasComContagem() = kotlinx.coroutines.flow.flowOf(
                    listOf(CategoriaComContagem(origem, 3, 0), CategoriaComContagem(destino, 0, 0)),
                )

                override fun observeRegrasComUso() = kotlinx.coroutines.flow.flowOf(emptyList<RegraCategoriaComUso>())

                override suspend fun criarCategoria(nome: String, tipo: TipoCategoria) = com.pingo.core.data.ResultadoCategoria.NomeVazio

                override suspend fun renomearCategoria(publicId: String, novoNome: String) =
                    com.pingo.core.data.ResultadoCategoria.NaoEncontrada

                override suspend fun alternarArquivoCategoria(publicId: String, arquivada: Boolean) = Unit

                override suspend fun excluirCategoria(publicId: String, categoriaDestinoPublicId: String?) =
                    com.pingo.core.data.ResultadoExclusaoCategoria.DestinoObrigatorio

                override suspend fun excluirRegra(publicId: String) = Unit
            },
        )

        composeRule.setContent {
            PingoTheme {
                CategoriasERegrasScreen(onBack = {}, viewModel = vm)
            }
        }

        composeRule.onNodeWithText("Excluir").performClick()
        composeRule.onNodeWithTag("botao_confirmar_exclusao_migracao").assertIsNotEnabled()
        composeRule.onNodeWithText("Outros").performClick()
    }
}

package com.pingo.feature.home.impl

import android.content.Intent
import com.pingo.core.network.update.AtualizacaoDisponivel
import com.pingo.core.network.update.ControladorDeAtualizacaoApp
import com.pingo.core.network.update.EstadoAtualizacaoDoApp
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeAtualizacaoTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val agora = Instant.parse("2026-11-18T12:00:00Z")
    private val info = AtualizacaoDisponivel(
        versao = "0.5.0",
        tag = "v0.5.0",
        titulo = "Pingo 0.5.0",
        notas = "### Novidades\n- **Ícones reais**: logos oficiais dos bancos.",
        urlPaginaRelease = "https://github.com/oDaviML/pingo-android/releases/tag/v0.5.0",
        urlDownloadApk = "https://github.com/oDaviML/pingo-android/releases/download/v0.5.0/Pingo-0.5.0-release.apk",
        nomeArquivoApk = "Pingo-0.5.0-release.apk",
    )

    @Test
    fun `verifica atualizacao em segundo plano sem abrir modal automaticamente e abre ao tocar no icone`() = runTest {
        val controlador = FakeControladorDeAtualizacao(proximaVersao = info)
        val viewModel = HomeViewModel(
            acessoNotificacoes = FakeAcessoNotificacoes(concedido = true),
            saudeDaCaptura = FakeSaudeDaCaptura(),
            lancamentoRepository = FakeLancamentoRepository(emptyList()),
            contaRepository = FakeContaRepository(emptyList()),
            instrumentoRepository = FakeInstrumentoRepository(emptyList()),
            categoriaRepository = FakeCategoriaRepository(emptyList()),
            inicioFinanceiroProjection = FakeInicioFinanceiroProjection(),
            controladorDeAtualizacao = controlador,
            clock = Clock.fixed(agora, ZoneOffset.UTC),
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.temAtualizacaoDisponivel)
        assertFalse(viewModel.uiState.value.mostrarSheetAtualizacao)
        assertEquals(1, controlador.verificacoesEmSegundoPlano)

        viewModel.abrirAtualizacao()
        assertTrue(viewModel.uiState.value.mostrarSheetAtualizacao)

        viewModel.baixarAtualizacao(info)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.estadoAtualizacao is EstadoAtualizacaoDoApp.ProntoParaInstalar)

        viewModel.fecharAtualizacao()
        assertFalse(viewModel.uiState.value.mostrarSheetAtualizacao)
    }

    private class FakeControladorDeAtualizacao(private val proximaVersao: AtualizacaoDisponivel?) : ControladorDeAtualizacaoApp {
        var verificacoesEmSegundoPlano = 0
        override val versaoInstalada: String = "0.4.0"
        override val codigoVersaoInstalado: Long = 4L
        private val mutableEstado = MutableStateFlow<EstadoAtualizacaoDoApp>(EstadoAtualizacaoDoApp.Ocioso)
        override val estado: StateFlow<EstadoAtualizacaoDoApp> = mutableEstado

        override suspend fun verificarEmSegundoPlano() {
            verificacoesEmSegundoPlano++
            mutableEstado.value = proximaVersao?.let { EstadoAtualizacaoDoApp.Disponivel(it) }
                ?: EstadoAtualizacaoDoApp.Atualizado(versaoInstalada)
        }

        override suspend fun verificarManualmente() {
            verificarEmSegundoPlano()
        }

        override suspend fun baixar(info: AtualizacaoDisponivel) {
            mutableEstado.value = EstadoAtualizacaoDoApp.ProntoParaInstalar(info, File("Pingo-0.5.0-release.apk"), false)
        }

        override fun atualizarPermissaoInstalacao() = Unit

        override fun intentPermissaoFontesDesconhecidas(): Intent = Intent()

        override fun intentInstalacaoApk(arquivoApk: File): Intent = Intent()
    }
}

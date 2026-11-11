package com.pingo.feature.capturestatus.impl

import com.pingo.core.data.RegraParserRepository
import com.pingo.core.datastore.AlertaDeCaptura
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.ContadorCaptura
import com.pingo.core.datastore.ContadoresDoMes
import com.pingo.core.datastore.EstadoAtualizacaoRegras
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.EstadoListenerPersistido
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.BateriaDoAparelho
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.MonitorDaCaptura
import com.pingo.core.notifications.NotificacoesDoSistema
import com.pingo.core.notifications.ResultadoAtualizacaoRegras
import com.pingo.core.notifications.SituacaoDaCaptura
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

class CaptureStatusViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val agora = Instant.parse("2026-11-10T15:30:00Z")
    private val mes = YearMonth.of(2026, 11)
    private val clock = Clock.fixed(agora, ZoneOffset.UTC)

    @Test
    fun `estado ativo reflete contadores leituras versao e permissoes`() = runTest {
        val dataSource = dataSourceAtiva()
        dataSource.registrarLeitura(InstituicaoFinanceira.ITAU, agora.minus(Duration.ofMinutes(3)))
        repetir(dataSource, ContadorCaptura.RECONHECIDAS, 184)
        repetir(dataSource, ContadorCaptura.PENDENTES, 12)
        repetir(dataSource, ContadorCaptura.IGNORADAS, 37)
        repetir(dataSource, ContadorCaptura.NAO_ENTENDIDAS, 3)

        val viewModel = viewModel(dataSource = dataSource, versao = 1)

        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(SituacaoDaCaptura.ATIVA, estado.situacao)
        assertFalse(estado.parada)
        assertEquals(ContadoresDaCaptura(reconhecidas = 184, paraRevisao = 12, ignoradas = 37, naoEntendidas = 3), estado.contadores)
        assertEquals(5, estado.leiturasPorApp.size)
        assertEquals(InstituicaoFinanceira.NUBANK, estado.leiturasPorApp.first().instituicao)
        assertNull(estado.leiturasPorApp.first().em)
        assertEquals(agora.minus(Duration.ofMinutes(3)), estado.leiturasPorApp.single { it.instituicao == InstituicaoFinanceira.ITAU }.em)
        assertEquals(1, estado.versaoDasRegras)
        assertEquals(agora.minus(Duration.ofMinutes(3)), estado.ultimaLeituraEm)
        assertEquals(InstituicaoFinanceira.ITAU, estado.ultimaLeituraInstituicao)
        assertTrue(estado.permissoes.all { it.concedida })
        assertNull(estado.ajustePrioritario)
    }

    @Test
    fun `sem acesso as notificacoes a tela mostra captura parada`() = runTest {
        val viewModel = viewModel(
            dataSource = dataSourceAtiva(),
            acessoConcedido = false,
        )

        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(SituacaoDaCaptura.PARADA, estado.situacao)
        assertTrue(estado.parada)
        assertEquals(MotivoParada.PERMISSAO_REVOGADA, estado.motivo)
        assertEquals(ItemDePermissao.ACESSO_A_NOTIFICACOES, estado.ajustePrioritario)
    }

    @Test
    fun `quarenta e oito horas sem leitura sinalizam parada na tela`() = runTest {
        val dataSource = dataSourceAtiva(observandoDesde = agora.minus(Duration.ofDays(4)))

        val viewModel = viewModel(dataSource = dataSource)

        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(SituacaoDaCaptura.SEM_LEITURA, estado.situacao)
        assertTrue(estado.parada)
        assertEquals(agora.minus(Duration.ofDays(4)), estado.semLeituraDesde)
    }

    @Test
    fun `estado parado preserva motivo e referencia do silencio`() = runTest {
        val dataSource = dataSourceAtiva(observandoDesde = agora.minus(Duration.ofDays(4)))
        dataSource.registrarParado(MotivoParada.DESCONECTADO, agora.minus(Duration.ofHours(2)))

        val viewModel = viewModel(dataSource = dataSource)

        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(SituacaoDaCaptura.PARADA, estado.situacao)
        assertTrue(estado.parada)
        assertEquals(MotivoParada.DESCONECTADO, estado.motivo)
        assertEquals(agora.minus(Duration.ofDays(4)), estado.semLeituraDesde)
    }

    @Test
    fun `pendente sem instrumento nao sinaliza captura parada`() = runTest {
        val dataSource = dataSourceAtiva()
        dataSource.registrarLeitura(InstituicaoFinanceira.INTER, agora.minus(Duration.ofMinutes(1)))
        repetir(dataSource, ContadorCaptura.PENDENTES, 1)

        val viewModel = viewModel(dataSource = dataSource)

        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(SituacaoDaCaptura.ATIVA, estado.situacao)
        assertFalse(estado.parada)
        assertEquals(1, estado.contadores.paraRevisao)
        assertEquals(0, estado.contadores.reconhecidas)
    }

    @Test
    fun `reconectar aciona o controller e atualiza a mensagem`() = runTest {
        val controller = FakeCapturaController(resultado = EstadoListenerPersistido(EstadoListener.ATIVO))
        val viewModel = viewModel(dataSource = dataSourceAtiva(), controller = controller)
        advanceUntilIdle()

        viewModel.reconectar()
        advanceUntilIdle()

        assertEquals(1, controller.reconexoes)
        assertEquals("Captura reconectada.", viewModel.uiState.value.mensagem)
        assertFalse(viewModel.uiState.value.reconectando)
    }

    @Test
    fun `reconectar sem sucesso pede para verificar as permissoes`() = runTest {
        val controller = FakeCapturaController(
            resultado = EstadoListenerPersistido(EstadoListener.PARADO, MotivoParada.PERMISSAO_REVOGADA),
        )
        val viewModel = viewModel(dataSource = dataSourceAtiva(), controller = controller)
        advanceUntilIdle()

        viewModel.reconectar()
        advanceUntilIdle()

        assertEquals(1, controller.reconexoes)
        assertEquals("Não foi possível reconectar. Verifique as permissões.", viewModel.uiState.value.mensagem)
    }

    @Test
    fun `atualizar regras forca o download e avisa a versao`() = runTest {
        val atualizacao = FakeAtualizacaoDeRegras(ResultadoAtualizacaoRegras.Atualizado(2))
        val viewModel = viewModel(dataSource = dataSourceAtiva(), atualizacao = atualizacao)
        advanceUntilIdle()

        viewModel.atualizarRegras()
        advanceUntilIdle()

        assertEquals(1, atualizacao.chamadas)
        assertEquals("Regras atualizadas para a versão 2.", viewModel.uiState.value.mensagem)
        assertFalse(viewModel.uiState.value.atualizandoRegras)
    }

    @Test
    fun `atualizar regras sem novidade informa a versao atual`() = runTest {
        val atualizacao = FakeAtualizacaoDeRegras(ResultadoAtualizacaoRegras.SemAlteracao(1))
        val viewModel = viewModel(dataSource = dataSourceAtiva(), atualizacao = atualizacao)
        advanceUntilIdle()

        viewModel.atualizarRegras()
        advanceUntilIdle()

        assertEquals("As regras já estão na versão 1.", viewModel.uiState.value.mensagem)
    }

    @Test
    fun `falha ao atualizar regras avisa sem quebrar a tela`() = runTest {
        val atualizacao = FakeAtualizacaoDeRegras(ResultadoAtualizacaoRegras.Falha("sem_conexao"))
        val dataSource = dataSourceAtiva()
        dataSource.registrarFalhaAoAtualizarRegras("sem_conexao", agora)
        val viewModel = viewModel(dataSource = dataSource, atualizacao = atualizacao)
        advanceUntilIdle()

        viewModel.atualizarRegras()
        advanceUntilIdle()

        assertEquals("Não foi possível atualizar as regras.", viewModel.uiState.value.mensagem)
        assertEquals("sem_conexao", viewModel.uiState.value.falhaAoAtualizarRegras)
        assertFalse(viewModel.uiState.value.atualizandoRegras)
    }

    @Test
    fun `atualizar estados recarrega permissoes e diagnostico`() = runTest {
        val acesso = FakeAcessoNotificacoes(concedido = true)
        val viewModel = viewModel(dataSource = dataSourceAtiva(), acesso = acesso)
        advanceUntilIdle()
        assertEquals(SituacaoDaCaptura.ATIVA, viewModel.uiState.value.situacao)

        acesso.concedido = false
        viewModel.atualizarEstados()
        advanceUntilIdle()

        assertEquals(SituacaoDaCaptura.PARADA, viewModel.uiState.value.situacao)
        assertFalse(viewModel.uiState.value.permissoes.single { it.item == ItemDePermissao.ACESSO_A_NOTIFICACOES }.concedida)
    }

    private suspend fun dataSourceAtiva(observandoDesde: Instant = agora.minus(Duration.ofHours(2))): FakeCapturaDataSource =
        FakeCapturaDataSource().apply {
            registrarAtivo(observandoDesde)
        }

    private suspend fun repetir(dataSource: FakeCapturaDataSource, contador: ContadorCaptura, vezes: Int) {
        repeat(vezes) { dataSource.incrementarContador(mes, contador) }
    }

    private fun viewModel(
        dataSource: FakeCapturaDataSource,
        acesso: FakeAcessoNotificacoes = FakeAcessoNotificacoes(concedido = true),
        versao: Int? = 1,
        controller: FakeCapturaController = FakeCapturaController(EstadoListenerPersistido(EstadoListener.ATIVO)),
        atualizacao: FakeAtualizacaoDeRegras = FakeAtualizacaoDeRegras(ResultadoAtualizacaoRegras.SemAlteracao(1)),
        acessoConcedido: Boolean = acesso.concedido,
    ): CaptureStatusViewModel {
        acesso.concedido = acessoConcedido

        return CaptureStatusViewModel(
            capturaDataSource = dataSource,
            saudeDaCaptura = MonitorDaCaptura(dataSource, clock),
            regraParserRepository = FakeRegraParserRepository(versao),
            atualizacaoDeRegras = atualizacao,
            capturaController = controller,
            acessoNotificacoes = acesso,
            notificacoesDoSistema = FakeNotificacoesDoSistema(habilitadas = true),
            bateriaDoAparelho = FakeBateriaDoAparelho(semRestricao = true),
            clock = clock,
        )
    }

    private class FakeAcessoNotificacoes(var concedido: Boolean) : AcessoNotificacoes {
        override fun concedido(): Boolean = concedido
    }

    private class FakeNotificacoesDoSistema(private val habilitadas: Boolean) : NotificacoesDoSistema {
        override fun habilitadas(): Boolean = habilitadas
    }

    private class FakeBateriaDoAparelho(private val semRestricao: Boolean) : BateriaDoAparelho {
        override fun semRestricaoDeBateria(): Boolean = semRestricao
    }

    private class FakeAtualizacaoDeRegras(private val resultado: ResultadoAtualizacaoRegras) : AtualizacaoDeRegras {
        var chamadas: Int = 0
            private set

        override suspend fun atualizar(): ResultadoAtualizacaoRegras {
            chamadas += 1
            return resultado
        }
    }

    private class FakeCapturaController(private val resultado: EstadoListenerPersistido) : CapturaController {
        var reconexoes: Int = 0
            private set

        override fun iniciarServico() = Unit

        override fun pararServico() = Unit

        override fun registrarConectado() = Unit

        override fun registrarDesconectado() = Unit

        override suspend fun reconectar(): EstadoListenerPersistido {
            reconexoes += 1
            return resultado
        }

        override suspend fun reconciliar(): EstadoListenerPersistido = resultado
    }

    private class FakeRegraParserRepository(private val versao: Int?) : RegraParserRepository {
        override fun observeRegrasAtivas(): Flow<List<RegraParser>> = flowOf(emptyList())

        override fun observeVersaoVigente(): Flow<Int?> = flowOf(versao)

        override suspend fun versoesEmCache(): Map<InstituicaoFinanceira, Int> = emptyMap()

        override suspend fun buscar(publicId: String): RegraParser? = null

        override suspend fun salvar(regra: RegraParser) = Unit

        override suspend fun salvarTodas(regras: List<RegraParser>) = Unit

        override suspend fun substituirVigentes(regras: List<RegraParser>) = Unit
    }

    private class FakeCapturaDataSource : CapturaDataSource {
        private val estado = MutableStateFlow(EstadoListenerPersistido(EstadoListener.PARADO))
        private val leituras = MutableStateFlow<Map<InstituicaoFinanceira, Instant>>(emptyMap())
        private val contadores = MutableStateFlow<Map<Pair<YearMonth, ContadorCaptura>, Int>>(emptyMap())
        private val observando = MutableStateFlow<Instant?>(null)
        private val regras = MutableStateFlow(EstadoAtualizacaoRegras())
        private val alerta = MutableStateFlow<AlertaDeCaptura?>(null)

        override val estadoListener: Flow<EstadoListenerPersistido> = estado.asStateFlow()

        override val ultimaLeituraPorApp: Flow<Map<InstituicaoFinanceira, Instant>> = leituras.asStateFlow()

        override val observandoDesde: Flow<Instant?> = observando.asStateFlow()

        override val estadoAtualizacaoRegras: Flow<EstadoAtualizacaoRegras> = regras.asStateFlow()

        override val alertaDeCaptura: Flow<AlertaDeCaptura?> = alerta.asStateFlow()

        override fun contadoresDoMes(mes: YearMonth): Flow<ContadoresDoMes> = flowOf(
            ContadoresDoMes(
                mes = mes,
                reconhecidas = valor(mes, ContadorCaptura.RECONHECIDAS),
                pendentes = valor(mes, ContadorCaptura.PENDENTES),
                ignoradas = valor(mes, ContadorCaptura.IGNORADAS),
                naoEntendidas = valor(mes, ContadorCaptura.NAO_ENTENDIDAS),
            ),
        )

        private fun valor(mes: YearMonth, contador: ContadorCaptura): Int = contadores.value[mes to contador] ?: 0

        override suspend fun registrarAtivo(em: Instant) {
            if (estado.value.estado != EstadoListener.ATIVO || observando.value == null) {
                observando.value = em
            }
            estado.value = EstadoListenerPersistido(EstadoListener.ATIVO, motivo = null, atualizadoEm = em)
        }

        override suspend fun registrarParado(motivo: MotivoParada, em: Instant) {
            estado.value = EstadoListenerPersistido(EstadoListener.PARADO, motivo, em)
        }

        override suspend fun registrarLeitura(instituicao: InstituicaoFinanceira, em: Instant) {
            leituras.value = leituras.value + (instituicao to em)
        }

        override suspend fun incrementarContador(mes: YearMonth, contador: ContadorCaptura) {
            contadores.value = contadores.value + ((mes to contador) to (valor(mes, contador) + 1))
        }

        override suspend fun registrarRegrasAtualizadas(em: Instant) {
            regras.value = EstadoAtualizacaoRegras(atualizadoEm = em)
        }

        override suspend fun registrarFalhaAoAtualizarRegras(motivo: String, em: Instant) {
            regras.value = regras.value.copy(falhaEm = em, motivoFalha = motivo)
        }

        override suspend fun registrarAlertaDeCaptura(referencia: Instant, em: Instant) {
            alerta.value = AlertaDeCaptura(referencia, em)
        }

        override suspend fun clear() {
            estado.value = EstadoListenerPersistido(EstadoListener.PARADO)
            leituras.value = emptyMap()
            contadores.value = emptyMap()
            observando.value = null
            regras.value = EstadoAtualizacaoRegras()
            alerta.value = null
        }
    }
}

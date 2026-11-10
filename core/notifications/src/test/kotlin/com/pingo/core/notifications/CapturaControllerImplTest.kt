package com.pingo.core.notifications

import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.EstadoListenerPersistido
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.testing.FakeAppDispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CapturaControllerImplTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val dispatcher = StandardTestDispatcher()
    private val dataSource = FakeCapturaDataSource()
    private val acesso = FakeAcessoNotificacoes()
    private val rebind = FakeRebindDeListener()
    private val servico = FakeServicoDeCaptura()
    private val conexao = ConexaoDoListener()
    private val escopo = EscopoDeCaptura(FakeAppDispatchers(dispatcher))
    private val controller = capturaController(dataSource)

    @Test
    fun registrarConectadoPersisteAtivoEIniciaServico() = runTest(dispatcher) {
        controller.registrarConectado()
        advanceUntilIdle()

        val estado = dataSource.estadoListener.first()
        assertEquals(EstadoListener.ATIVO, estado.estado)
        assertEquals(null, estado.motivo)
        assertEquals(clock.instant(), estado.atualizadoEm)
        assertEquals(1, servico.iniciados)
        assertEquals(true, conexao.conexao.value)
    }

    @Test
    fun registrarDesconectadoComAcessoMantidoMarcaDesconectadoESolicitaRebind() = runTest(dispatcher) {
        acesso.concedido = true
        controller.registrarConectado()
        advanceUntilIdle()

        controller.registrarDesconectado()
        advanceUntilIdle()

        val estado = dataSource.estadoListener.first()
        assertEquals(EstadoListener.PARADO, estado.estado)
        assertEquals(MotivoParada.DESCONECTADO, estado.motivo)
        assertEquals(1, rebind.solicitacoes)
        assertEquals(false, conexao.conexao.value)
    }

    @Test
    fun registrarDesconectadoSemAcessoMarcaPermissaoRevogadaSemRebind() = runTest(dispatcher) {
        acesso.concedido = false

        controller.registrarDesconectado()
        advanceUntilIdle()

        val estado = dataSource.estadoListener.first()
        assertEquals(EstadoListener.PARADO, estado.estado)
        assertEquals(MotivoParada.PERMISSAO_REVOGADA, estado.motivo)
        assertEquals(0, rebind.solicitacoes)
    }

    @Test
    fun registrarDesconectadoSolicitaRebindSemEsperarPersistencia() = runTest(dispatcher) {
        acesso.concedido = true
        val bloqueado = DataSourceComEscritaBloqueada()
        val controlador = capturaController(bloqueado)

        controlador.registrarDesconectado()

        assertEquals(1, rebind.solicitacoes)

        advanceUntilIdle()

        assertTrue(bloqueado.persistenciaIniciada.isCompleted)
        assertEquals(0, bloqueado.persistenciasConcluidas)

        bloqueado.liberar()
        advanceUntilIdle()

        assertEquals(1, bloqueado.persistenciasConcluidas)
        assertEquals(MotivoParada.DESCONECTADO, bloqueado.estadoListener.first().motivo)
    }

    @Test
    fun desconexaoComPersistenciaAtrasadaNaoSobrescreveConexaoNova() = runTest(dispatcher) {
        acesso.concedido = true
        val bloqueado = DataSourceComEscritaBloqueada()
        val controlador = capturaController(bloqueado)

        controlador.registrarDesconectado()
        advanceUntilIdle()
        assertTrue(bloqueado.persistenciaIniciada.isCompleted)

        controlador.registrarConectado()
        advanceUntilIdle()

        bloqueado.liberar()
        advanceUntilIdle()

        val estado = bloqueado.estadoListener.first()
        assertEquals(EstadoListener.ATIVO, estado.estado)
        assertEquals(null, estado.motivo)
        assertEquals(true, conexao.conexao.value)
    }

    @Test
    fun reconectarComAcessoIniciaServicoESolicitaRebind() = runTest(dispatcher) {
        acesso.concedido = true
        dataSource.registrarParado(MotivoParada.DESCONECTADO, clock.instant())

        controller.reconectar()

        assertEquals(1, servico.iniciados)
        assertEquals(1, rebind.solicitacoes)
    }

    @Test
    fun reconectarComConexaoVivaRestauraAtivoSemRebind() = runTest(dispatcher) {
        acesso.concedido = true
        controller.registrarConectado()
        advanceUntilIdle()
        dataSource.registrarParado(MotivoParada.PERMISSAO_REVOGADA, clock.instant())

        val estado = controller.reconectar()

        assertEquals(EstadoListener.ATIVO, estado.estado)
        assertEquals(null, estado.motivo)
        assertEquals(2, servico.iniciados)
        assertEquals(0, rebind.solicitacoes)
    }

    @Test
    fun reconectarSemAcessoMarcaPermissaoRevogadaSemRebind() = runTest(dispatcher) {
        acesso.concedido = false

        val estado = controller.reconectar()

        assertEquals(EstadoListener.PARADO, estado.estado)
        assertEquals(MotivoParada.PERMISSAO_REVOGADA, estado.motivo)
        assertEquals(0, rebind.solicitacoes)
        assertEquals(0, servico.iniciados)
    }

    @Test
    fun reconciliarComAcessoIniciaServicoSemRebind() = runTest(dispatcher) {
        acesso.concedido = true

        controller.reconciliar()

        assertEquals(1, servico.iniciados)
        assertEquals(0, rebind.solicitacoes)
    }

    @Test
    fun reconciliarComConexaoVivaRestauraAtivo() = runTest(dispatcher) {
        acesso.concedido = true
        controller.registrarConectado()
        advanceUntilIdle()
        dataSource.registrarParado(MotivoParada.DESCONECTADO, clock.instant())

        val estado = controller.reconciliar()

        assertEquals(EstadoListener.ATIVO, estado.estado)
        assertEquals(0, rebind.solicitacoes)
    }

    @Test
    fun reconciliarSemAcessoMarcaPermissaoRevogada() = runTest(dispatcher) {
        acesso.concedido = false
        dataSource.registrarAtivo(clock.instant())

        val estado = controller.reconciliar()

        assertEquals(EstadoListener.PARADO, estado.estado)
        assertEquals(MotivoParada.PERMISSAO_REVOGADA, estado.motivo)
        assertEquals(0, servico.iniciados)
    }

    private fun capturaController(dataSource: CapturaDataSource) = CapturaControllerImpl(
        capturaDataSource = dataSource,
        acessoNotificacoes = acesso,
        rebindDeListener = rebind,
        servicoDeCaptura = servico,
        conexaoDoListener = conexao,
        escopoDeCaptura = escopo,
        clock = clock,
    )

    private class DataSourceComEscritaBloqueada(private val interno: FakeCapturaDataSource = FakeCapturaDataSource()) :
        CapturaDataSource by interno {
        val persistenciaIniciada = CompletableDeferred<Unit>()
        private val liberacao = CompletableDeferred<Unit>()

        var persistenciasConcluidas = 0
            private set

        fun liberar() {
            liberacao.complete(Unit)
        }

        override suspend fun registrarParado(motivo: MotivoParada, em: Instant) {
            persistenciaIniciada.complete(Unit)
            liberacao.await()
            interno.registrarParado(motivo, em)
            persistenciasConcluidas += 1
        }
    }

    private class FakeAcessoNotificacoes(var concedido: Boolean = true) : AcessoNotificacoes {
        override fun concedido(): Boolean = concedido
    }

    private class FakeRebindDeListener : RebindDeListener {
        var solicitacoes: Int = 0
            private set

        override fun solicitar(): Boolean {
            solicitacoes += 1
            return true
        }
    }

    private class FakeServicoDeCaptura : ServicoDeCaptura {
        var iniciados: Int = 0
            private set

        var parados: Int = 0
            private set

        override fun iniciar() {
            iniciados += 1
        }

        override fun parar() {
            parados += 1
        }
    }
}

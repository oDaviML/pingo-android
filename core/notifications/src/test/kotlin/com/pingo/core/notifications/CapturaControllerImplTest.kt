package com.pingo.core.notifications

import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.EstadoListenerPersistido
import com.pingo.core.datastore.MotivoParada
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class CapturaControllerImplTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val dataSource = FakeCapturaDataSource()
    private val acesso = FakeAcessoNotificacoes()
    private val rebind = FakeRebindDeListener()
    private val servico = FakeServicoDeCaptura()
    private val conexao = ConexaoDoListener()
    private val controller = CapturaControllerImpl(dataSource, acesso, rebind, servico, conexao, clock)

    @Test
    fun registrarConectadoPersisteAtivoEIniciaServico() = runTest {
        controller.registrarConectado()

        val estado = dataSource.estadoListener.first()
        assertEquals(EstadoListener.ATIVO, estado.estado)
        assertEquals(null, estado.motivo)
        assertEquals(clock.instant(), estado.atualizadoEm)
        assertEquals(1, servico.iniciados)
        assertEquals(true, conexao.conexao.value)
    }

    @Test
    fun registrarDesconectadoComAcessoMantidoMarcaDesconectadoESolicitaRebind() = runTest {
        acesso.concedido = true
        controller.registrarConectado()

        controller.registrarDesconectado()

        val estado = dataSource.estadoListener.first()
        assertEquals(EstadoListener.PARADO, estado.estado)
        assertEquals(MotivoParada.DESCONECTADO, estado.motivo)
        assertEquals(1, rebind.solicitacoes)
        assertEquals(false, conexao.conexao.value)
    }

    @Test
    fun registrarDesconectadoSemAcessoMarcaPermissaoRevogadaSemRebind() = runTest {
        acesso.concedido = false

        controller.registrarDesconectado()

        val estado = dataSource.estadoListener.first()
        assertEquals(EstadoListener.PARADO, estado.estado)
        assertEquals(MotivoParada.PERMISSAO_REVOGADA, estado.motivo)
        assertEquals(0, rebind.solicitacoes)
    }

    @Test
    fun reconectarComAcessoIniciaServicoESolicitaRebind() = runTest {
        acesso.concedido = true
        dataSource.registrarParado(MotivoParada.DESCONECTADO, clock.instant())

        controller.reconectar()

        assertEquals(1, servico.iniciados)
        assertEquals(1, rebind.solicitacoes)
    }

    @Test
    fun reconectarComConexaoVivaRestauraAtivoSemRebind() = runTest {
        acesso.concedido = true
        controller.registrarConectado()
        dataSource.registrarParado(MotivoParada.PERMISSAO_REVOGADA, clock.instant())

        val estado = controller.reconectar()

        assertEquals(EstadoListener.ATIVO, estado.estado)
        assertEquals(null, estado.motivo)
        assertEquals(2, servico.iniciados)
        assertEquals(0, rebind.solicitacoes)
    }

    @Test
    fun reconectarSemAcessoMarcaPermissaoRevogadaSemRebind() = runTest {
        acesso.concedido = false

        val estado = controller.reconectar()

        assertEquals(EstadoListener.PARADO, estado.estado)
        assertEquals(MotivoParada.PERMISSAO_REVOGADA, estado.motivo)
        assertEquals(0, rebind.solicitacoes)
        assertEquals(0, servico.iniciados)
    }

    @Test
    fun reconciliarComAcessoIniciaServicoSemRebind() = runTest {
        acesso.concedido = true

        controller.reconciliar()

        assertEquals(1, servico.iniciados)
        assertEquals(0, rebind.solicitacoes)
    }

    @Test
    fun reconciliarComConexaoVivaRestauraAtivo() = runTest {
        acesso.concedido = true
        controller.registrarConectado()
        dataSource.registrarParado(MotivoParada.DESCONECTADO, clock.instant())

        val estado = controller.reconciliar()

        assertEquals(EstadoListener.ATIVO, estado.estado)
        assertEquals(0, rebind.solicitacoes)
    }

    @Test
    fun reconciliarSemAcessoMarcaPermissaoRevogada() = runTest {
        acesso.concedido = false
        dataSource.registrarAtivo(clock.instant())

        val estado = controller.reconciliar()

        assertEquals(EstadoListener.PARADO, estado.estado)
        assertEquals(MotivoParada.PERMISSAO_REVOGADA, estado.motivo)
        assertEquals(0, servico.iniciados)
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

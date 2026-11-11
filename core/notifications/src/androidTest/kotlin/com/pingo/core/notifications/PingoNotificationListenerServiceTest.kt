package com.pingo.core.notifications

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.datastore.AlertaDeCaptura
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.ContadorCaptura
import com.pingo.core.datastore.ContadoresDoMes
import com.pingo.core.datastore.EstadoAtualizacaoRegras
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.EstadoListenerPersistido
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.InstituicaoFinanceira
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class PingoNotificationListenerServiceTest {
    @Test
    fun desconexaoSolicitaRebindAntesDaPersistenciaESobreviveAoDestroy() {
        val dataSource = CapturaDataSourceDeTeste()
        val rebind = RebindDeListenerDeTeste()
        val servico = servico(rebind = rebind, dataSource = dataSource, acessoConcedido = true)

        servico.onListenerDisconnected()

        assertEquals(1, rebind.solicitacoes)
        runBlocking { withTimeout(TEMPO_DE_ESPERA) { dataSource.persistenciaIniciada.await() } }

        servico.onDestroy()
        dataSource.liberarPersistencia()

        val estado = runBlocking {
            withTimeout(TEMPO_DE_ESPERA) {
                dataSource.estadoListener.first { it.estado == EstadoListener.PARADO }
            }
        }

        assertEquals(MotivoParada.DESCONECTADO, estado.motivo)
        assertEquals(1, rebind.solicitacoes)
    }

    @Test
    fun semPermissaoRegistraParadoComMotivoSemRebind() {
        val dataSource = CapturaDataSourceDeTeste()
        val rebind = RebindDeListenerDeTeste()
        val servico = servico(rebind = rebind, dataSource = dataSource, acessoConcedido = false)

        servico.onListenerDisconnected()
        dataSource.liberarPersistencia()

        val estado = runBlocking {
            withTimeout(TEMPO_DE_ESPERA) {
                dataSource.estadoListener.first { it.estado == EstadoListener.PARADO }
            }
        }

        assertEquals(MotivoParada.PERMISSAO_REVOGADA, estado.motivo)
        assertEquals(0, rebind.solicitacoes)
    }

    @Test
    fun destroySemCallbackPrevioSolicitaRebindEPersisteParado() {
        val dataSource = CapturaDataSourceDeTeste()
        val rebind = RebindDeListenerDeTeste()
        val servico = servico(rebind = rebind, dataSource = dataSource, acessoConcedido = true)

        servico.onDestroy()
        dataSource.liberarPersistencia()

        val estado = runBlocking {
            withTimeout(TEMPO_DE_ESPERA) {
                dataSource.estadoListener.first { it.estado == EstadoListener.PARADO }
            }
        }

        assertEquals(1, rebind.solicitacoes)
        assertEquals(MotivoParada.DESCONECTADO, estado.motivo)
    }

    @Test
    fun desconexaoComPersistenciaAtrasadaNaoSobrescreveConexaoNova() {
        val dataSource = CapturaDataSourceDeTeste()
        val rebind = RebindDeListenerDeTeste()
        val conexao = ConexaoDoListener()
        val servico = servico(rebind = rebind, dataSource = dataSource, acessoConcedido = true, conexao = conexao)

        servico.onListenerDisconnected()
        runBlocking { withTimeout(TEMPO_DE_ESPERA) { dataSource.persistenciaIniciada.await() } }

        servico.onListenerConnected()
        dataSource.liberarPersistencia()

        runBlocking { withTimeout(TEMPO_DE_ESPERA) { dataSource.ativoGravado.await() } }

        val estado = runBlocking { dataSource.estadoListener.first() }

        assertEquals(EstadoListener.ATIVO, estado.estado)
        assertNull(estado.motivo)
        assertTrue(conexao.conexao.value)
    }

    private fun servico(
        rebind: RebindDeListenerDeTeste,
        dataSource: CapturaDataSourceDeTeste,
        acessoConcedido: Boolean,
        conexao: ConexaoDoListener = ConexaoDoListener(),
    ): PingoNotificationListenerService {
        val controlador = CapturaControllerImpl(
            capturaDataSource = dataSource,
            acessoNotificacoes = object : AcessoNotificacoes {
                override fun concedido(): Boolean = acessoConcedido
            },
            rebindDeListener = rebind,
            servicoDeCaptura = object : ServicoDeCaptura {
                override fun iniciar() = Unit

                override fun parar() = Unit
            },
            conexaoDoListener = conexao,
            escopoDeCaptura = EscopoDeCaptura(DefaultAppDispatchers),
            clock = Clock.systemUTC(),
        )

        return PingoNotificationListenerService().apply { capturaController = controlador }
    }

    private class RebindDeListenerDeTeste : RebindDeListener {
        var solicitacoes: Int = 0
            private set

        override fun solicitar(): Boolean {
            solicitacoes += 1
            return true
        }
    }

    private class CapturaDataSourceDeTeste : CapturaDataSource {
        private val estado = MutableStateFlow(EstadoListenerPersistido(EstadoListener.ATIVO))
        private val liberacao = CompletableDeferred<Unit>()
        private val ativo = CompletableDeferred<Unit>()

        val persistenciaIniciada = CompletableDeferred<Unit>()

        val ativoGravado: CompletableDeferred<Unit>
            get() = ativo

        override val estadoListener: Flow<EstadoListenerPersistido> = estado

        override val ultimaLeituraPorApp: Flow<Map<InstituicaoFinanceira, Instant>> = flowOf(emptyMap())

        override val observandoDesde: Flow<Instant?> = flowOf(null)

        override val estadoAtualizacaoRegras: Flow<EstadoAtualizacaoRegras> = flowOf(EstadoAtualizacaoRegras())

        override val alertaDeCaptura: Flow<AlertaDeCaptura?> = flowOf(null)

        fun liberarPersistencia() {
            liberacao.complete(Unit)
        }

        override fun contadoresDoMes(mes: YearMonth): Flow<ContadoresDoMes> = flowOf(ContadoresDoMes(mes))

        override suspend fun registrarAtivo(em: Instant) {
            estado.value = EstadoListenerPersistido(EstadoListener.ATIVO, motivo = null, atualizadoEm = em)
            ativo.complete(Unit)
        }

        override suspend fun registrarParado(motivo: MotivoParada, em: Instant) {
            persistenciaIniciada.complete(Unit)
            liberacao.await()
            estado.value = EstadoListenerPersistido(EstadoListener.PARADO, motivo, em)
        }

        override suspend fun registrarLeitura(instituicao: InstituicaoFinanceira, em: Instant) = Unit

        override suspend fun incrementarContador(mes: YearMonth, contador: ContadorCaptura) = Unit

        override suspend fun registrarRegrasAtualizadas(em: Instant) = Unit

        override suspend fun registrarFalhaAoAtualizarRegras(motivo: String, em: Instant) = Unit

        override suspend fun registrarAlertaDeCaptura(referencia: Instant, em: Instant) = Unit

        override suspend fun clear() = Unit
    }

    private companion object {
        const val TEMPO_DE_ESPERA = 5_000L
    }
}

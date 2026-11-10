package com.pingo.core.notifications

import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.EstadoListenerPersistido
import com.pingo.core.datastore.MotivoParada
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapturaControllerImpl @Inject constructor(
    private val capturaDataSource: CapturaDataSource,
    private val acessoNotificacoes: AcessoNotificacoes,
    private val rebindDeListener: RebindDeListener,
    private val servicoDeCaptura: ServicoDeCaptura,
    private val conexaoDoListener: ConexaoDoListener,
    private val clock: Clock,
) : CapturaController {
    override fun iniciarServico() = servicoDeCaptura.iniciar()

    override fun pararServico() = servicoDeCaptura.parar()

    override suspend fun registrarConectado() {
        conexaoDoListener.marcarConectado()
        capturaDataSource.registrarAtivo(clock.instant())
        servicoDeCaptura.iniciar()
    }

    override suspend fun registrarDesconectado() {
        conexaoDoListener.marcarDesconectado()

        if (!acessoNotificacoes.concedido()) {
            capturaDataSource.registrarParado(MotivoParada.PERMISSAO_REVOGADA, clock.instant())
            return
        }

        capturaDataSource.registrarParado(MotivoParada.DESCONECTADO, clock.instant())
        rebindDeListener.solicitar()
    }

    override suspend fun reconectar(): EstadoListenerPersistido {
        if (acessoNotificacoes.concedido()) {
            servicoDeCaptura.iniciar()
            if (conexaoDoListener.conexao.value) {
                capturaDataSource.registrarAtivo(clock.instant())
            } else {
                rebindDeListener.solicitar()
            }
        } else {
            capturaDataSource.registrarParado(MotivoParada.PERMISSAO_REVOGADA, clock.instant())
        }

        return capturaDataSource.estadoListener.first()
    }

    override suspend fun reconciliar(): EstadoListenerPersistido {
        if (acessoNotificacoes.concedido()) {
            servicoDeCaptura.iniciar()
            if (conexaoDoListener.conexao.value) {
                capturaDataSource.registrarAtivo(clock.instant())
            }
        } else {
            capturaDataSource.registrarParado(MotivoParada.PERMISSAO_REVOGADA, clock.instant())
        }

        return capturaDataSource.estadoListener.first()
    }
}

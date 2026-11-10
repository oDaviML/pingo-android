package com.pingo.core.notifications

import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.EstadoListenerPersistido
import com.pingo.core.datastore.MotivoParada
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val escopoDeCaptura: EscopoDeCaptura,
    private val clock: Clock,
) : CapturaController {
    private val persistencia = Mutex()

    override fun iniciarServico() = servicoDeCaptura.iniciar()

    override fun pararServico() = servicoDeCaptura.parar()

    override fun registrarConectado() {
        val versao = conexaoDoListener.marcarConectado()
        servicoDeCaptura.iniciar()
        escopoDeCaptura.lancar {
            persistirSeVigente(versao) {
                capturaDataSource.registrarAtivo(clock.instant())
            }
        }
    }

    override fun registrarDesconectado() {
        val versao = conexaoDoListener.marcarDesconectado()
        val acessoConcedido = acessoNotificacoes.concedido()

        if (acessoConcedido) {
            rebindDeListener.solicitar()
        }

        escopoDeCaptura.lancar {
            persistirSeVigente(versao) {
                capturaDataSource.registrarParado(motivoDaParada(acessoConcedido), clock.instant())
            }
        }
    }

    override suspend fun reconectar(): EstadoListenerPersistido {
        val conexao = conexaoDoListener.instantaneo()

        if (acessoNotificacoes.concedido()) {
            servicoDeCaptura.iniciar()
            if (conexao.conectado) {
                persistirSeVigente(conexao.versao) {
                    capturaDataSource.registrarAtivo(clock.instant())
                }
            } else {
                rebindDeListener.solicitar()
            }
        } else {
            persistirSeVigente(conexao.versao) {
                capturaDataSource.registrarParado(MotivoParada.PERMISSAO_REVOGADA, clock.instant())
            }
        }

        return capturaDataSource.estadoListener.first()
    }

    override suspend fun reconciliar(): EstadoListenerPersistido {
        val conexao = conexaoDoListener.instantaneo()

        if (acessoNotificacoes.concedido()) {
            servicoDeCaptura.iniciar()
            if (conexao.conectado) {
                persistirSeVigente(conexao.versao) {
                    capturaDataSource.registrarAtivo(clock.instant())
                }
            }
        } else {
            persistirSeVigente(conexao.versao) {
                capturaDataSource.registrarParado(MotivoParada.PERMISSAO_REVOGADA, clock.instant())
            }
        }

        return capturaDataSource.estadoListener.first()
    }

    private suspend fun persistirSeVigente(versao: Long, persistir: suspend () -> Unit) {
        persistencia.withLock {
            if (conexaoDoListener.vigente(versao)) {
                persistir()
            }
        }
    }

    private fun motivoDaParada(acessoConcedido: Boolean): MotivoParada =
        if (acessoConcedido) MotivoParada.DESCONECTADO else MotivoParada.PERMISSAO_REVOGADA
}

package com.pingo.core.notifications

class AndroidNotificationHealthCheck(
    private val acessoNotificacoes: AcessoNotificacoes,
    private val estadoDoServico: EstadoDoServicoDeCaptura,
) : NotificationHealthCheck {
    override fun isListenerEnabled(): Boolean = acessoNotificacoes.concedido()

    override fun isCaptureServiceRunning(): Boolean = estadoDoServico.emExecucao.value
}

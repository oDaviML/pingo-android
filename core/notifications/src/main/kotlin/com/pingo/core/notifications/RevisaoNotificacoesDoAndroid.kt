package com.pingo.core.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RevisaoNotificacoesDoAndroid @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val revisaoPorNotificacao: RevisaoPorNotificacao,
    private val notificacoesDoSistema: NotificacoesDoSistema,
) : RevisaoNotificacoes {
    override suspend fun habilitada(): Boolean = revisaoPorNotificacao.habilitada()

    override fun postar(pendencia: PendenciaParaNotificar, precisaCorrigir: Boolean): Boolean {
        if (!notificacoesDoSistema.habilitadas()) {
            return false
        }

        if (!notificacoesPermitidas()) {
            return false
        }

        NotificacoesDeRevisao.criarCanal(context)

        return try {
            NotificationManagerCompat.from(context).notify(
                NotificacoesDeRevisao.idDe(pendencia.publicId),
                NotificacoesDeRevisao.construir(context, pendencia, precisaCorrigir),
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun cancelar(publicId: String) {
        val gerenciador = NotificationManagerCompat.from(context)
        gerenciador.activeNotifications
            .filter { it.notification.channelId == NotificacoesDeRevisao.CANAL }
            .filter { it.notification.extras.getString(NotificacoesDeRevisao.EXTRA_PUBLIC_ID) == publicId }
            .forEach { gerenciador.cancel(it.tag, it.id) }
        gerenciador.cancel(NotificacoesDeRevisao.idDe(publicId))
    }

    override fun cancelarAusentes(publicIds: Set<String>) {
        val gerenciador = NotificationManagerCompat.from(context)
        gerenciador.activeNotifications
            .filter { it.notification.channelId == NotificacoesDeRevisao.CANAL }
            .filter { it.notification.extras.getString(NotificacoesDeRevisao.EXTRA_PUBLIC_ID) != null }
            .filter { it.notification.extras.getString(NotificacoesDeRevisao.EXTRA_PUBLIC_ID) !in publicIds }
            .forEach { gerenciador.cancel(it.tag, it.id) }
    }

    private fun notificacoesPermitidas(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

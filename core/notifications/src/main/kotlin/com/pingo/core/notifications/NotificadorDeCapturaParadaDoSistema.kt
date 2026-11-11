package com.pingo.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificadorDeCapturaParadaDoSistema @Inject constructor(@param:ApplicationContext private val context: Context) :
    NotificadorDeCapturaParada {
    override fun capturaParada() {
        NotificacoesDeCaptura.criarCanais(context)
        val gerenciador = NotificationManagerCompat.from(context)

        if (!gerenciador.areNotificationsEnabled()) {
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        gerenciador.notify(
            NotificacoesDeCaptura.ID_ALERTA_PARADA,
            NotificacoesDeCaptura.alertaDeCapturaParada(context, destino = destino()),
        )
    }

    private fun destino(): PendingIntent? = context.packageManager
        .getLaunchIntentForPackage(context.packageName)
        ?.let { intencao ->
            PendingIntent.getActivity(
                context,
                0,
                intencao,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
}

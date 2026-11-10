package com.pingo.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificacoesDeCaptura {
    const val ID_SERVICO = 1001

    private const val CANAL = "captura"
    private const val CANAL_NOME = "Captura automática"
    private const val CANAL_DESCRICAO = "Aviso fixo enquanto o Pingo acompanha as notificações dos seus bancos."

    fun criarCanal(context: Context) {
        val gerenciador = context.getSystemService(NotificationManager::class.java)
        val canal = NotificationChannel(CANAL, CANAL_NOME, NotificationManager.IMPORTANCE_LOW).apply {
            description = CANAL_DESCRICAO
            setShowBadge(false)
        }
        gerenciador.createNotificationChannel(canal)
    }

    fun persistente(context: Context, destino: PendingIntent?): Notification = NotificationCompat.Builder(context, CANAL)
        .setSmallIcon(R.drawable.ic_captura)
        .setContentTitle("Pingo está capturando")
        .setContentText("Seus bancos são lidos só no aparelho.")
        .setContentIntent(destino)
        .setOngoing(true)
        .setSilent(true)
        .setShowWhen(false)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .build()
}

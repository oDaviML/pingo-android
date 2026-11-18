package com.pingo.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificacoesDeCaptura {
    const val ID_SERVICO = 1001
    const val ID_ALERTA_PARADA = 1002

    private const val CANAL = "captura"
    private const val CANAL_ALERTAS = "alertas"
    private const val CANAL_ALERTAS_NOME = "Avisos da captura"
    private const val CANAL_ALERTAS_DESCRICAO = "Quando o Pingo fica dois dias sem ler notificações dos seus bancos."

    fun criarCanais(context: Context) {
        val gerenciador = context.getSystemService(NotificationManager::class.java)
        val canalDeAlertas = NotificationChannel(CANAL_ALERTAS, CANAL_ALERTAS_NOME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = CANAL_ALERTAS_DESCRICAO
        }
        gerenciador.createNotificationChannel(canalDeAlertas)
        gerenciador.deleteNotificationChannel(CANAL)
        gerenciador.cancel(ID_SERVICO)
    }

    fun alertaDeCapturaParada(context: Context, destino: PendingIntent?): Notification = NotificationCompat.Builder(context, CANAL_ALERTAS)
        .setSmallIcon(R.drawable.ic_captura)
        .setContentTitle("A captura parou")
        .setContentText("Nenhuma notificação lida há 2 dias. Toque para ver o status.")
        .setContentIntent(destino)
        .setAutoCancel(true)
        .setShowWhen(false)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
}

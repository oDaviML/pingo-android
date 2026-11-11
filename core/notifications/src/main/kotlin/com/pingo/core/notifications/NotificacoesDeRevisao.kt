package com.pingo.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.pingo.core.ui.Valores

object NotificacoesDeRevisao {
    const val CANAL = "revisao"
    const val ACAO_CONFIRMAR = "com.pingo.action.CONFIRMAR_LANCAMENTO_CAPTURADO"
    const val EXTRA_PUBLIC_ID = "com.pingo.extra.LANCAMENTO_CAPTURADO"
    const val PADRAO_DEEP_LINK_DETALHE = "pingo://detalhe/{publicId}"
    const val TITULO = "Lançamento para revisar"
    const val SEM_CATEGORIA = "Sem categoria"
    const val SEM_ESTABELECIMENTO = "Lançamento"
    const val AVISO_CORRIGIR = "Escolha o cartão para confirmar. Toque em Corrigir."
    const val ACAO_CONFIRMAR_LABEL = "Confirmar"
    const val ACAO_CORRIGIR_LABEL = "Corrigir"

    private const val CANAL_NOME = "Revisão de lançamentos"
    private const val CANAL_DESCRICAO = "Avisos para confirmar ou corrigir lançamentos capturados."
    private const val ESQUEMA = "pingo"
    private const val HOST_DETALHE = "detalhe"
    private const val ID_BASE = 2000
    private const val ID_FAIXA = 1_000_000

    fun criarCanal(context: Context) {
        val gerenciador = context.getSystemService(NotificationManager::class.java)
        val canal = NotificationChannel(CANAL, CANAL_NOME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = CANAL_DESCRICAO
        }
        gerenciador.createNotificationChannel(canal)
    }

    fun idDe(publicId: String): Int = ID_BASE + (publicId.hashCode() and Int.MAX_VALUE) % ID_FAIXA

    fun detalheUri(publicId: String): Uri = Uri.Builder()
        .scheme(ESQUEMA)
        .authority(HOST_DETALHE)
        .appendPath(publicId)
        .build()

    fun conteudo(pendencia: PendenciaParaNotificar, precisaCorrigir: Boolean): ConteudoDaRevisao = ConteudoDaRevisao(
        titulo = TITULO,
        resumo = listOf(
            pendencia.estabelecimento?.takeIf { it.isNotBlank() } ?: SEM_ESTABELECIMENTO,
            Valores.formatar(pendencia.valorCentavos),
            "Categoria sugerida: ${pendencia.categoria ?: SEM_CATEGORIA}",
        ).joinToString(" · "),
        aviso = if (precisaCorrigir) AVISO_CORRIGIR else null,
    )

    fun construir(context: Context, pendencia: PendenciaParaNotificar, precisaCorrigir: Boolean): Notification {
        val conteudo = conteudo(pendencia, precisaCorrigir)

        return NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_captura)
            .setContentTitle(conteudo.titulo)
            .setContentText(conteudo.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(conteudo.textoLongo))
            .setContentIntent(corrigirIntencao(context, pendencia.publicId))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, ACAO_CONFIRMAR_LABEL, confirmarIntencao(context, pendencia.publicId))
            .addAction(0, ACAO_CORRIGIR_LABEL, corrigirIntencao(context, pendencia.publicId))
            .addExtras(Bundle().apply { putString(EXTRA_PUBLIC_ID, pendencia.publicId) })
            .build()
    }

    fun intencaoDeCorrecao(context: Context, publicId: String): Intent {
        val intencao = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_VIEW)

        return intencao.apply {
            action = Intent.ACTION_VIEW
            data = detalheUri(publicId)
            putExtra(EXTRA_PUBLIC_ID, publicId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun confirmarIntencao(context: Context, publicId: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        idDe(publicId),
        Intent(context, RevisarPendenciaReceiver::class.java)
            .setAction(ACAO_CONFIRMAR)
            .putExtra(EXTRA_PUBLIC_ID, publicId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun corrigirIntencao(context: Context, publicId: String): PendingIntent = PendingIntent.getActivity(
        context,
        idDe(publicId),
        intencaoDeCorrecao(context, publicId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

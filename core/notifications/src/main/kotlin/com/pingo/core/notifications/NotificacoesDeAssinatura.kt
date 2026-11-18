package com.pingo.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pingo.core.model.Assinatura
import com.pingo.core.ui.Valores
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ConteudoAvisoAssinatura(val titulo: String, val texto: String)

object NotificacoesDeAssinatura {
    const val CANAL = "pingo_avisos_assinaturas"
    const val EXTRA_ASSINATURA_PUBLIC_ID = "com.pingo.extra.ASSINATURA_PUBLIC_ID"
    const val TITULO = "Assinatura prevista"

    private const val CANAL_NOME = "Avisos de assinaturas"
    private const val CANAL_DESCRICAO = "Lembretes antes de cobranças previstas de assinaturas confirmadas."
    private const val SEM_ESTABELECIMENTO = "Assinatura"
    private const val MAIN_ACTIVITY_CLASS = "com.pingo.MainActivity"
    private const val ID_BASE = 3000
    private const val ID_FAIXA = 1_000_000
    private val FORMATO_DATA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

    fun criarCanal(context: Context) {
        val gerenciador = context.getSystemService(NotificationManager::class.java)
        val canal = NotificationChannel(CANAL, CANAL_NOME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = CANAL_DESCRICAO
        }
        gerenciador.createNotificationChannel(canal)
    }

    fun idDe(publicId: String): Int = ID_BASE + (publicId.hashCode() and Int.MAX_VALUE) % ID_FAIXA

    fun conteudo(assinatura: Assinatura, dataPrevista: LocalDate): ConteudoAvisoAssinatura {
        val estabelecimento = assinatura.estabelecimento.trim()
            .ifBlank { assinatura.estabelecimentoNormalizado.trim() }
            .ifBlank { SEM_ESTABELECIMENTO }
        val valorFormatado = Valores.formatar(assinatura.valorReferenciaCentavos)
        val dataFormatada = FORMATO_DATA.format(dataPrevista)
        return ConteudoAvisoAssinatura(
            titulo = TITULO,
            texto = "$estabelecimento · $valorFormatado · Cobrança prevista em $dataFormatada",
        )
    }

    fun intencaoDeAbertura(context: Context): Intent {
        val base = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(context.packageName, MAIN_ACTIVITY_CLASS)
            }
        return Intent(base).apply {
            flags = flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    fun abrirInicioIntencao(context: Context, publicId: String): PendingIntent {
        val intencao = intencaoDeAbertura(context).apply {
            putExtra(EXTRA_ASSINATURA_PUBLIC_ID, publicId)
        }
        return PendingIntent.getActivity(
            context,
            idDe(publicId),
            intencao,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun construir(context: Context, assinatura: Assinatura, dataPrevista: LocalDate): Notification {
        val conteudo = conteudo(assinatura, dataPrevista)
        return NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_captura)
            .setContentTitle(conteudo.titulo)
            .setContentText(conteudo.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(conteudo.texto))
            .setContentIntent(abrirInicioIntencao(context, assinatura.publicId))
            .setAutoCancel(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addExtras(Bundle().apply { putString(EXTRA_ASSINATURA_PUBLIC_ID, assinatura.publicId) })
            .build()
    }

    fun postar(context: Context, assinatura: Assinatura, dataPrevista: LocalDate): Boolean {
        val gerenciador = NotificationManagerCompat.from(context)
        val permitida = gerenciador.areNotificationsEnabled() && permissaoPostNotificationsConcedida(context)
        if (!permitida) {
            return false
        }

        criarCanal(context)
        return try {
            gerenciador.notify(idDe(assinatura.publicId), construir(context, assinatura, dataPrevista))
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun permissaoPostNotificationsConcedida(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

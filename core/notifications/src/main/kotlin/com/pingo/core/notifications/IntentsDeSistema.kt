package com.pingo.core.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object IntentsDeSistema {
    fun acessoAsNotificacoes(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun notificacoesDoApp(pacote: String): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pacote)

    fun ajustesDeBateria(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun detalhesDoApp(pacote: String): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pacote, null))

    fun inicioAutomatico(fabricante: FabricanteDoSistema): Intent? = when (fabricante) {
        FabricanteDoSistema.XIAOMI -> Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        )

        else -> null
    }

    fun abrir(context: Context, intencao: Intent) {
        val comFlags = Intent(intencao).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { context.startActivity(comFlags) }.onFailure {
            context.startActivity(detalhesDoApp(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

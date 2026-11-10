package com.pingo.core.notifications

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcessoNotificacoesDoSistema @Inject constructor(@param:ApplicationContext private val context: Context) : AcessoNotificacoes {
    override fun concedido(): Boolean {
        val habilitados = Settings.Secure.getString(context.contentResolver, CHAVE_ACESSO_A_LISTENERS) ?: return false
        val esperado = ComponentName(context, PingoNotificationListenerService::class.java)
        return habilitados.split(SEPARADOR).any { componente -> mesmoComponente(componente, esperado) }
    }

    private fun mesmoComponente(valor: String, esperado: ComponentName): Boolean {
        val componente = ComponentName.unflattenFromString(valor) ?: return false
        return componente.packageName.equals(esperado.packageName, true) &&
            componente.className.equals(esperado.className, true)
    }

    private companion object {
        const val SEPARADOR = ':'
    }
}

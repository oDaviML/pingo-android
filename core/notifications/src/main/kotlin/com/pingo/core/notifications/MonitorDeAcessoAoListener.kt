package com.pingo.core.notifications

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings

internal const val CHAVE_ACESSO_A_LISTENERS = "enabled_notification_listeners"

class MonitorDeAcessoAoListener(private val context: Context, private val aoAlterar: () -> Unit) :
    ContentObserver(Handler(Looper.getMainLooper())) {
    private var registrado = false

    fun iniciar() {
        if (registrado) {
            return
        }

        context.contentResolver.registerContentObserver(Settings.Secure.getUriFor(CHAVE_ACESSO_A_LISTENERS), false, this)
        registrado = true
    }

    fun parar() {
        if (!registrado) {
            return
        }

        context.contentResolver.unregisterContentObserver(this)
        registrado = false
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        aoAlterar()
    }
}

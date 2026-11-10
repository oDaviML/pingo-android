package com.pingo.core.notifications

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServicoDeCapturaDoAndroid @Inject constructor(@param:ApplicationContext private val context: Context) : ServicoDeCaptura {
    override fun iniciar() {
        runCatching { ContextCompat.startForegroundService(context, Intent(context, CapturaForegroundService::class.java)) }
    }

    override fun parar() {
        context.stopService(Intent(context, CapturaForegroundService::class.java))
    }
}

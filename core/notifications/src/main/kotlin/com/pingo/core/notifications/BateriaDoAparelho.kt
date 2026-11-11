package com.pingo.core.notifications

import android.content.Context
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface BateriaDoAparelho {
    fun semRestricaoDeBateria(): Boolean
}

@Singleton
class BateriaDoAparelhoDoAndroid @Inject constructor(@param:ApplicationContext private val context: Context) : BateriaDoAparelho {
    override fun semRestricaoDeBateria(): Boolean {
        val gerenciador = context.getSystemService(PowerManager::class.java) ?: return false
        return gerenciador.isIgnoringBatteryOptimizations(context.packageName)
    }
}

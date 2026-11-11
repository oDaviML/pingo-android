package com.pingo.feature.capturestatus.impl

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AjustesDaCapturaTest {
    private val pacote = "com.pingo"

    @Test
    fun acessoAsNotificacoesAbreOsAjustesDeListener() {
        val intencao = intencaoPara(ItemDePermissao.ACESSO_A_NOTIFICACOES, pacote)

        assertEquals(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, intencao.action)
    }

    @Test
    fun notificacoesDoPingoAbremOsAjustesDoApp() {
        val intencao = intencaoPara(ItemDePermissao.NOTIFICACOES_DO_PINGO, pacote)

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intencao.action)
        assertEquals(pacote, intencao.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }

    @Test
    fun bateriaAbreOsAjustesDeOtimizacao() {
        val intencao = intencaoPara(ItemDePermissao.BATERIA, pacote)

        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, intencao.action)
    }
}

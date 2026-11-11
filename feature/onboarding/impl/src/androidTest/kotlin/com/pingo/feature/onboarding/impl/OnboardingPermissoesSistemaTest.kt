package com.pingo.feature.onboarding.impl

import android.Manifest
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.notifications.FabricanteDoSistema
import com.pingo.core.notifications.IntentsDeSistema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingPermissoesSistemaTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun intencaoDeAcessoAbreATelaDeAcessoAsNotificacoes() {
        assertEquals(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, IntentsDeSistema.acessoAsNotificacoes().action)
    }

    @Test
    fun intencaoDeNotificacoesDoAppApontaParaOPacote() {
        val intencao = IntentsDeSistema.notificacoesDoApp(contexto.packageName)

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intencao.action)
        assertEquals(contexto.packageName, intencao.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }

    @Test
    fun intencaoDeBateriaAbreOsAjustesDeBateria() {
        assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, IntentsDeSistema.ajustesDeBateria().action)
    }

    @Test
    fun inicioAutomaticoDaXiaomiApontaParaOSecurityCenter() {
        val intencao = IntentsDeSistema.inicioAutomatico(FabricanteDoSistema.XIAOMI)

        assertEquals("com.miui.securitycenter", intencao?.component?.packageName)
        assertNull(IntentsDeSistema.inicioAutomatico(FabricanteDoSistema.SAMSUNG))
    }

    @Test
    fun solicitacaoPedeOPostNotificationsERegistraOResultado() {
        val permissao = PermissaoDeNotificacaoDoAndroid(contexto, precisaPedir = { true })
        var pedida: String? = null
        var resultado: Boolean? = null

        permissao.conectarLancador { pedida = it }
        permissao.solicitar { resultado = it }

        assertEquals(Manifest.permission.POST_NOTIFICATIONS, pedida)

        permissao.aoResultadoDaSolicitacao(concedida = false)

        assertEquals(false, resultado)
    }

    @Test
    fun semPrecisarPedirRespondeNaHoraSemAbrirDialogo() {
        val permissao = PermissaoDeNotificacaoDoAndroid(contexto, precisaPedir = { false })
        var pedida: String? = null
        var resultado: Boolean? = null

        permissao.conectarLancador { pedida = it }
        permissao.solicitar { resultado = it }

        assertNull(pedida)
        assertEquals(permissao.concedida(), resultado)
    }
}

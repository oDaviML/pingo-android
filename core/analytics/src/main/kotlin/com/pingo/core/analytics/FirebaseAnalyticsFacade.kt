package com.pingo.core.analytics

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsFacade @Inject internal constructor(private val canal: CanalTelemetria) : AnalyticsFacade {
    private val habilitada = AtomicBoolean(false)

    fun estaHabilitada(): Boolean = habilitada.get()

    override fun setAnalyticsEnabled(enabled: Boolean) {
        habilitada.set(enabled)
        canal.atualizarColeta(enabled)
    }

    override fun logEvent(name: String, attributes: Map<String, String>) {
        if (!habilitada.get()) return
        val eventoSeguro = FiltroPrivacidadeTelemetria.filtrarEvento(name) ?: return
        val atributosSeguros = FiltroPrivacidadeTelemetria.filtrarAtributos(attributes)
        canal.registrarEvento(eventoSeguro, atributosSeguros)
    }

    override fun recordError(throwable: Throwable) {
        if (!habilitada.get()) return
        canal.registrarExcecao(FiltroPrivacidadeTelemetria.sanitizarExcecao(throwable))
    }
}

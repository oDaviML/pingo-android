package com.pingo.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CanalFirebaseAndroid @Inject constructor(@param:ApplicationContext private val context: Context) : CanalTelemetria {
    override fun atualizarColeta(habilitada: Boolean) {
        if (!firebaseDisponivel(inicializarSeAusente = habilitada)) return

        runCatching {
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(habilitada)
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = habilitada
        }
    }

    override fun registrarEvento(nome: String, atributos: Map<String, String>) {
        if (!firebaseDisponivel(inicializarSeAusente = false)) return

        runCatching {
            val bundle = Bundle().apply {
                atributos.forEach { (chave, valor) -> putString(chave, valor) }
            }
            FirebaseAnalytics.getInstance(context).logEvent(nome, bundle)
        }
    }

    override fun registrarExcecao(erro: Throwable) {
        if (!firebaseDisponivel(inicializarSeAusente = false)) return

        runCatching {
            FirebaseCrashlytics.getInstance().recordException(erro)
        }
    }

    private fun firebaseDisponivel(inicializarSeAusente: Boolean): Boolean = runCatching {
        FirebaseApp.getApps(context).isNotEmpty() || (inicializarSeAusente && FirebaseApp.initializeApp(context) != null)
    }.getOrDefault(false)
}

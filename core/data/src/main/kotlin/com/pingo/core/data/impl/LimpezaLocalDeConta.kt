package com.pingo.core.data.impl

import com.pingo.core.analytics.AnalyticsFacade
import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.database.PingoDatabase
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.PermissoesDataSource
import com.pingo.core.datastore.UserPreferencesDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LimpezaLocalDeConta @Inject constructor(
    private val database: PingoDatabase,
    private val preferences: UserPreferencesDataSource,
    private val capturaDataSource: CapturaDataSource,
    private val permissoesDataSource: PermissoesDataSource,
    private val agendadorDeAvisos: AgendadorDeAvisosOpcional,
    private val analytics: AnalyticsFacade,
) {
    constructor(
        database: PingoDatabase,
        preferences: UserPreferencesDataSource,
        capturaDataSource: CapturaDataSource,
        permissoesDataSource: PermissoesDataSource,
        agendadorDeAvisos: AgendadorDeAvisosOpcional,
    ) : this(
        database = database,
        preferences = preferences,
        capturaDataSource = capturaDataSource,
        permissoesDataSource = permissoesDataSource,
        agendadorDeAvisos = agendadorDeAvisos,
        analytics = AnalyticsSemOpcao,
    )

    suspend fun limpar() {
        analytics.setAnalyticsEnabled(false)
        agendadorDeAvisos.cancelarTodos()
        database.clearAllTables()
        preferences.clear()
        capturaDataSource.clear()
        permissoesDataSource.clear()
    }

    private object AnalyticsSemOpcao : AnalyticsFacade {
        override fun setAnalyticsEnabled(enabled: Boolean) = Unit

        override fun logEvent(name: String, attributes: Map<String, String>) = Unit

        override fun recordError(throwable: Throwable) = Unit
    }
}

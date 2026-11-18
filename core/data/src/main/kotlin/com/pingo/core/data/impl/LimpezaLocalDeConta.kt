package com.pingo.core.data.impl

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
) {
    suspend fun limpar() {
        agendadorDeAvisos.cancelarTodos()
        database.clearAllTables()
        preferences.clear()
        capturaDataSource.clear()
        permissoesDataSource.clear()
    }
}

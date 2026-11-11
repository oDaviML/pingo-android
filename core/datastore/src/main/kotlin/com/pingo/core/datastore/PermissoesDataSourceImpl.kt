package com.pingo.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.permissoesPreferencesDataStore by preferencesDataStore(name = "permissoes_preferences")

@Singleton
class PermissoesDataSourceImpl @Inject constructor(@param:ApplicationContext private val context: Context) : PermissoesDataSource {
    override val acessoAsNotificacoes: Flow<PermissaoDoPingo> =
        context.permissoesPreferencesDataStore.data.map { preferences -> preferences.permissao(Keys.ACESSO_AS_NOTIFICACOES) }

    override val notificacoesDoPingo: Flow<PermissaoDoPingo> =
        context.permissoesPreferencesDataStore.data.map { preferences -> preferences.permissao(Keys.NOTIFICACOES_DO_PINGO) }

    override suspend fun registrarAcessoAsNotificacoes(permissao: PermissaoDoPingo) {
        context.permissoesPreferencesDataStore.edit { preferences ->
            preferences[Keys.ACESSO_AS_NOTIFICACOES] = permissao.name
        }
    }

    override suspend fun registrarNotificacoesDoPingo(permissao: PermissaoDoPingo) {
        context.permissoesPreferencesDataStore.edit { preferences ->
            preferences[Keys.NOTIFICACOES_DO_PINGO] = permissao.name
        }
    }

    override suspend fun clear() {
        context.permissoesPreferencesDataStore.edit { preferences -> preferences.clear() }
    }

    private fun Preferences.permissao(chave: Preferences.Key<String>): PermissaoDoPingo =
        this[chave]?.let { valor -> runCatching { PermissaoDoPingo.valueOf(valor) }.getOrNull() }
            ?: PermissaoDoPingo.NAO_PERGUNTADA

    private companion object {
        object Keys {
            val ACESSO_AS_NOTIFICACOES = stringPreferencesKey("acesso_as_notificacoes")
            val NOTIFICACOES_DO_PINGO = stringPreferencesKey("notificacoes_do_pingo")
        }
    }
}

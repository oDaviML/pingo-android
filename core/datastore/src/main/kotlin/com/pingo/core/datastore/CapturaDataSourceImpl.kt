package com.pingo.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pingo.core.model.InstituicaoFinanceira
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

private val Context.capturaPreferencesDataStore by preferencesDataStore(name = "captura_preferences")

@Singleton
class CapturaDataSourceImpl @Inject constructor(@param:ApplicationContext private val context: Context) : CapturaDataSource {
    override val estadoListener: Flow<EstadoListenerPersistido> =
        context.capturaPreferencesDataStore.data.map(::estadoDe)

    override val ultimaLeituraPorApp: Flow<Map<InstituicaoFinanceira, Instant>> =
        context.capturaPreferencesDataStore.data.map(::leiturasDe)

    override fun contadoresDoMes(mes: YearMonth): Flow<ContadoresDoMes> = context.capturaPreferencesDataStore.data.map { preferences ->
        ContadoresDoMes(
            mes = mes,
            reconhecidas = preferences.contador(mes, ContadorCaptura.RECONHECIDAS),
            pendentes = preferences.contador(mes, ContadorCaptura.PENDENTES),
            ignoradas = preferences.contador(mes, ContadorCaptura.IGNORADAS),
            naoEntendidas = preferences.contador(mes, ContadorCaptura.NAO_ENTENDIDAS),
        )
    }

    override suspend fun registrarAtivo(em: Instant) {
        context.capturaPreferencesDataStore.edit { preferences ->
            preferences[Keys.ESTADO_LISTENER] = EstadoListener.ATIVO.name
            preferences.remove(Keys.MOTIVO_PARADA)
            preferences[Keys.ATUALIZADO_EM] = em.toString()
        }
    }

    override suspend fun registrarParado(motivo: MotivoParada, em: Instant) {
        context.capturaPreferencesDataStore.edit { preferences ->
            preferences[Keys.ESTADO_LISTENER] = EstadoListener.PARADO.name
            preferences[Keys.MOTIVO_PARADA] = motivo.name
            preferences[Keys.ATUALIZADO_EM] = em.toString()
        }
    }

    override suspend fun registrarLeitura(instituicao: InstituicaoFinanceira, em: Instant) {
        context.capturaPreferencesDataStore.edit { preferences ->
            preferences[Keys.ultimaLeitura(instituicao)] = em.toString()
        }
    }

    override suspend fun incrementarContador(mes: YearMonth, contador: ContadorCaptura) {
        context.capturaPreferencesDataStore.edit { preferences ->
            val chave = Keys.contador(mes, contador)
            preferences[chave] = preferences.contador(mes, contador) + 1
        }
    }

    override suspend fun clear() {
        context.capturaPreferencesDataStore.edit { preferences -> preferences.clear() }
    }

    private fun estadoDe(preferences: Preferences): EstadoListenerPersistido {
        val estado = preferences[Keys.ESTADO_LISTENER]
            ?.let { valor -> runCatching { EstadoListener.valueOf(valor) }.getOrNull() }
            ?: EstadoListener.PARADO
        val motivo = preferences[Keys.MOTIVO_PARADA]
            ?.let { valor -> runCatching { MotivoParada.valueOf(valor) }.getOrNull() }
        val atualizadoEm = preferences[Keys.ATUALIZADO_EM]
            ?.let { valor -> runCatching { Instant.parse(valor) }.getOrNull() }
        return EstadoListenerPersistido(estado = estado, motivo = motivo, atualizadoEm = atualizadoEm)
    }

    private fun leiturasDe(preferences: Preferences): Map<InstituicaoFinanceira, Instant> = InstituicaoFinanceira.entries
        .mapNotNull { instituicao ->
            preferences[Keys.ultimaLeitura(instituicao)]
                ?.let { valor -> runCatching { Instant.parse(valor) }.getOrNull() }
                ?.let { instante -> instituicao to instante }
        }
        .toMap()

    private fun Preferences.contador(mes: YearMonth, contador: ContadorCaptura): Int = this[Keys.contador(mes, contador)] ?: 0

    private companion object {
        object Keys {
            val ESTADO_LISTENER = stringPreferencesKey("estado_listener")
            val MOTIVO_PARADA = stringPreferencesKey("motivo_parada")
            val ATUALIZADO_EM = stringPreferencesKey("estado_atualizado_em")

            fun ultimaLeitura(instituicao: InstituicaoFinanceira) = stringPreferencesKey("ultima_leitura_${instituicao.name}")

            fun contador(mes: YearMonth, contador: ContadorCaptura) = intPreferencesKey("contador_${mes}_${contador.name}")
        }
    }
}

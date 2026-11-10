package com.pingo.sync

import androidx.room.withTransaction
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.SessionState
import com.pingo.core.data.categoria.CategoriaSeeder
import com.pingo.core.database.PingoDatabase
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.network.PingoApiClient
import com.pingo.core.network.sync.PublicIdAliasDto
import com.pingo.core.network.sync.PushChangesPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

fun interface SyncRunner {
    suspend fun run()
}

@Singleton
class SyncEngine @Inject constructor(
    private val api: PingoApiClient,
    private val database: PingoDatabase,
    private val preferences: UserPreferencesDataSource,
    private val authRepository: AuthRepository,
    private val deviceActivator: DeviceActivator,
    private val categoriaSeeder: CategoriaSeeder,
) : SyncRunner {
    private val applier = SyncApplier(database)
    private val syncMutex = Mutex()

    override suspend fun run() = syncMutex.withLock {
        if (authRepository.session.value != SessionState.LoggedIn || preferences.consentVersion() == null) {
            return@withLock
        }

        preferences.setSyncStatus(SyncStatus.RUNNING)

        try {
            runCatching { deviceActivator.activate() }.onFailure { exception ->
                if (exception is CancellationException) {
                    throw exception
                }
            }

            val inicio = Instant.now()
            val payload = coletar(preferences.pushCursor() ?: Instant.EPOCH)

            pull()
            categoriaSeeder.garantirPadroes(Instant.now())
            push(payload = payload, inicio = inicio)

            preferences.setLastSyncAt(Instant.now())
            preferences.setSyncStatus(SyncStatus.IDLE)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            preferences.setSyncStatus(SyncStatus.FAILED)
            throw exception
        }
    }

    private suspend fun pull() {
        val result = api.pullChanges(preferences.pullCursor())

        applier.apply(result)

        result.proximoCursor?.let { cursor -> preferences.setPullCursor(cursor) }
    }

    private suspend fun push(payload: PushChangesPayload, inicio: Instant) {
        if (payload.isEmpty) {
            preferences.setPushCursor(inicio)
            return
        }

        val result = api.pushChanges(payload)

        aplicarAliases(result.aliases)
        preferences.setPushCursor(inicio)
    }

    private suspend fun coletar(desde: Instant): PushChangesPayload = PushChangesPayload(
        contas = database.contaDao().atualizadasDesde(desde).map { it.toChangeDto() },
        instrumentos = database.instrumentoDao().atualizadosDesde(desde).map { it.toChangeDto() },
        categorias = database.categoriaDao().atualizadasDesde(desde).map { it.toChangeDto() },
        regrasCategoria = database.regraCategoriaDao().atualizadasDesde(desde).map { it.toChangeDto() },
        assinaturas = database.assinaturaDao().atualizadasDesde(desde).map { it.toChangeDto() },
        faturas = database.faturaDao().atualizadasDesde(desde).map { it.toChangeDto() },
        lancamentos = database.lancamentoDao().atualizadosDesde(desde).map { it.toChangeDto() },
    )

    private suspend fun aplicarAliases(aliases: List<PublicIdAliasDto>) {
        if (aliases.isEmpty()) {
            return
        }

        database.withTransaction {
            for (alias in aliases) {
                val duplicado = database.lancamentoDao().buscar(alias.publicId)
                val dono = database.lancamentoDao().buscar(alias.ownerPublicId)

                if (duplicado != null && dono != null) {
                    val textoDoDuplicado = duplicado.textoBruto
                    if (dono.textoBruto == null && textoDoDuplicado != null) {
                        database.lancamentoDao().definirTextoBruto(dono.publicId, textoDoDuplicado)
                    }
                    database.lancamentoDao().repointarCompraPai(de = duplicado.publicId, para = dono.publicId)
                    database.lancamentoDao().repointarEstorno(de = duplicado.publicId, para = dono.publicId)
                    database.faturaDao().repointarPagamento(de = duplicado.publicId, para = dono.publicId)
                    database.lancamentoDao().apagar(duplicado.publicId)
                }
            }
        }
    }
}

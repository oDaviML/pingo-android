package com.pingo.sync

import androidx.room.withTransaction
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.SessionState
import com.pingo.core.database.PingoDatabase
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.network.PingoApiClient
import com.pingo.core.network.sync.PublicIdAliasDto
import com.pingo.core.network.sync.PushChangesPayload
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
) : SyncRunner {
    private val applier = SyncApplier(database)

    override suspend fun run() {
        if (authRepository.session.value != SessionState.LoggedIn) {
            return
        }

        preferences.setSyncStatus(SyncStatus.RUNNING)

        try {
            pull()
            push()
            preferences.setLastSyncAt(Instant.now())
            preferences.setSyncStatus(SyncStatus.IDLE)
        } catch (exception: Exception) {
            preferences.setSyncStatus(SyncStatus.FAILED)
            throw exception
        }
    }

    private suspend fun pull() {
        val result = api.pullChanges(preferences.pullCursor())

        applier.apply(result)

        preferences.setPullCursor(result.proximoCursor)
    }

    private suspend fun push() {
        val inicio = Instant.now()
        val payload = coletar(preferences.pushCursor() ?: Instant.EPOCH)

        preferences.setPushCursor(inicio)

        if (payload.isEmpty) {
            return
        }

        val result = api.pushChanges(payload)

        aplicarAliases(result.aliases)
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
                    database.lancamentoDao().repointarCompraPai(de = duplicado.publicId, para = dono.publicId)
                    database.lancamentoDao().repointarEstorno(de = duplicado.publicId, para = dono.publicId)
                    database.faturaDao().repointarPagamento(de = duplicado.publicId, para = dono.publicId)
                    database.lancamentoDao().apagar(duplicado.publicId)
                }
            }
        }
    }
}

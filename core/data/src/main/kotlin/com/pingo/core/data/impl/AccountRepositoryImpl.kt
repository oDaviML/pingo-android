package com.pingo.core.data.impl

import com.pingo.core.auth.AuthRepository
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.AccountRepository
import com.pingo.core.database.PingoDatabase
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.network.PingoApiClient
import com.pingo.core.network.PingoApiException
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val api: PingoApiClient,
    private val authRepository: AuthRepository,
    private val database: PingoDatabase,
    private val preferences: UserPreferencesDataSource,
    private val dispatchers: AppDispatchers,
) : AccountRepository {
    override suspend fun recordConsent(version: String) = withContext(dispatchers.io) {
        val consent = api.recordConsent(version)

        preferences.setConsent(
            version = consent.version ?: version,
            instant = consent.consentedAt?.let { OffsetDateTime.parse(it).toInstant() } ?: Instant.now(),
        )
    }

    override suspend fun deleteAccount() = withContext(dispatchers.io) {
        try {
            api.deleteAccount()
        } catch (exception: PingoApiException) {
            if (exception.statusCode != HTTP_NOT_FOUND) {
                throw exception
            }
        }

        runCatching { database.clearAllTables() }
        runCatching { preferences.clear() }
        authRepository.logout()
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}

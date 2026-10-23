package com.pingo.core.data.impl

import com.pingo.core.auth.AuthRepository
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.AccountRepository
import com.pingo.core.database.PingoDatabase
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.network.PingoApiClient
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val api: PingoApiClient,
    private val authRepository: AuthRepository,
    private val database: PingoDatabase,
    private val preferences: UserPreferencesDataSource,
    private val dispatchers: AppDispatchers,
) : AccountRepository {
    override suspend fun recordConsent(version: String) = withContext(dispatchers.io) {
        api.recordConsent(version)
        preferences.setConsent(version = version, instant = Instant.now())
    }

    override suspend fun deleteAccount() = withContext(dispatchers.io) {
        api.deleteAccount()

        database.clearAllTables()
        preferences.clear()
        authRepository.logout()
    }
}

package com.pingo.core.data

interface AccountRepository {
    suspend fun recordConsent(version: String)

    suspend fun deleteAccount()
}

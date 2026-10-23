package com.pingo.core.network

import com.pingo.core.network.sync.PullChangesResult
import com.pingo.core.network.sync.PushChangesPayload
import com.pingo.core.network.sync.PushChangesResult

data class PingoApiConfig(val baseUrl: String, val connectTimeoutMillis: Long = 15_000)

interface PingoApiClient {
    suspend fun healthCheck(): Boolean

    suspend fun getProfile(): ApiProfile

    suspend fun pushChanges(payload: PushChangesPayload): PushChangesResult

    suspend fun pullChanges(cursor: String?): PullChangesResult
}

data class ApiProfile(
    val email: String,
    val nome: String?,
    val consentimentoLgpdEm: String?,
    val consentimentoLgpdVersao: String?,
    val aparelhoAtivoId: String?,
    val aparelhoAtivoNome: String?,
    val aparelhoAtivoEm: String?,
)

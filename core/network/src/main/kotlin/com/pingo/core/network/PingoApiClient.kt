package com.pingo.core.network

import com.pingo.core.network.rules.ParserRulesResponse
import com.pingo.core.network.sync.PullChangesResult
import com.pingo.core.network.sync.PushChangesPayload
import com.pingo.core.network.sync.PushChangesResult

data class PingoApiConfig(val baseUrl: String, val connectTimeoutMillis: Long = 15_000)

interface PingoApiClient {
    suspend fun healthCheck(): Boolean

    suspend fun getProfile(): ApiProfile

    suspend fun pushChanges(payload: PushChangesPayload): PushChangesResult

    suspend fun pullChanges(cursor: String?): PullChangesResult

    suspend fun getParserRules(): ParserRulesResponse

    suspend fun activateDevice(deviceId: String, deviceName: String?): ApiDevice

    suspend fun recordConsent(version: String): ApiConsent

    suspend fun deleteAccount()
}

data class ApiConsent(val consentedAt: String?, val version: String?)

data class ApiProfile(
    val email: String,
    val nome: String?,
    val consentimentoLgpdEm: String?,
    val consentimentoLgpdVersao: String?,
    val aparelhoAtivoId: String?,
    val aparelhoAtivoNome: String?,
    val aparelhoAtivoEm: String?,
)

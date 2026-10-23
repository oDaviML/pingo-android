package com.pingo.core.network

data class PingoApiConfig(val baseUrl: String, val connectTimeoutMillis: Long = 15_000)

interface PingoApiClient {
    suspend fun healthCheck(): Boolean

    suspend fun getProfile(): ApiProfile
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

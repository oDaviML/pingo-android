package com.pingo.core.network

data class PingoApiConfig(val baseUrl: String, val connectTimeoutMillis: Long)

interface PingoApiClient {
    suspend fun healthCheck(): Boolean
}

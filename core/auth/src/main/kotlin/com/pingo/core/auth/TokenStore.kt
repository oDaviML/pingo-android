package com.pingo.core.auth

interface TokenStore {
    suspend fun read(): StoredTokens?

    suspend fun write(tokens: StoredTokens)

    suspend fun clear()
}

data class StoredTokens(val accessToken: String, val refreshToken: String?, val expiresAtEpochMillis: Long)

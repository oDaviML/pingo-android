package com.pingo.core.auth

interface TokenStore {
    suspend fun readAccessToken(): String?

    suspend fun writeAccessToken(token: String)

    suspend fun clear()
}

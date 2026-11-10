package com.pingo.core.auth

import javax.inject.Inject

class UsuarioAutenticado @Inject constructor(private val tokenStore: TokenStore) {
    suspend fun identificador(): String? = tokenStore.read()?.accessToken?.let(JwtSubject::de)
}

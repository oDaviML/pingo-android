package com.pingo.core.auth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsuarioAutenticadoTest {
    @Test
    fun `identificador vem do sub do access token armazenado`() = runBlocking {
        val tokens = StoredTokens(jwtComPayload("""{"sub":"auth0|lucas"}"""), null, Long.MAX_VALUE)

        assertEquals("auth0|lucas", UsuarioAutenticado(FakeTokenStore(tokens)).identificador())
    }

    @Test
    fun `sem tokens armazenados nao ha identificador`() = runBlocking {
        assertNull(UsuarioAutenticado(FakeTokenStore(null)).identificador())
    }

    private class FakeTokenStore(private var tokens: StoredTokens?) : TokenStore {
        override suspend fun read(): StoredTokens? = tokens

        override suspend fun write(tokens: StoredTokens) {
            this.tokens = tokens
        }

        override suspend fun clear() {
            tokens = null
        }
    }
}

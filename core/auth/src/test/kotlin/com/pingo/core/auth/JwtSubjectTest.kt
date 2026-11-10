package com.pingo.core.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JwtSubjectTest {
    @Test
    fun `extrai o sub do corpo do access token`() {
        assertEquals("google-oauth2|116", JwtSubject.de(jwtComPayload("""{"sub":"google-oauth2|116","aud":"api"}""")))
    }

    @Test
    fun `token sem claim sub devolve nulo`() {
        assertNull(JwtSubject.de(jwtComPayload("""{"aud":"api"}""")))
    }

    @Test
    fun `token que nao e jwt devolve nulo`() {
        assertNull(JwtSubject.de("nao-e-um-jwt"))
    }

    @Test
    fun `corpo que nao e json devolve nulo`() {
        assertNull(JwtSubject.de(jwtComPayload("nao-e-json")))
    }
}

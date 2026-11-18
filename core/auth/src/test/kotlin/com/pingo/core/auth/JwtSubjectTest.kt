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

    @Test
    fun `extrai email e nome do token`() {
        val perfil = JwtSubject.perfil(jwtComPayload("""{"email":"ana@pingo.app","name":"Ana Paula"}"""))

        assertEquals(PerfilJwt(email = "ana@pingo.app", nome = "Ana Paula"), perfil)
    }

    @Test
    fun `usa given_name quando name estiver ausente`() {
        val perfil = JwtSubject.perfil(jwtComPayload("""{"email":"davi@pingo.app","given_name":"Davi"}"""))

        assertEquals(PerfilJwt(email = "davi@pingo.app", nome = "Davi"), perfil)
    }

    @Test
    fun `token sem email e sem nome devolve perfil nulo`() {
        assertNull(JwtSubject.perfil(jwtComPayload("""{"sub":"auth0|1"}""")))
    }
}

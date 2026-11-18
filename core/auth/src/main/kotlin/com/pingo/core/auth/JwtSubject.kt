package com.pingo.core.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

internal data class PerfilJwt(val email: String?, val nome: String?)

internal object JwtSubject {
    fun de(token: String): String? = payloadJson(token)?.get("sub")?.jsonPrimitive?.contentOrNull

    fun perfil(token: String): PerfilJwt? {
        val obj = payloadJson(token) ?: return null
        val email = obj["email"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        val nome = (obj["name"] ?: obj["given_name"])?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

        return if (email == null && nome == null) null else PerfilJwt(email = email, nome = nome)
    }

    private fun payloadJson(token: String): JsonObject? {
        val corpo = token.split('.').getOrNull(1) ?: return null

        return runCatching {
            val json = String(Base64.getUrlDecoder().decode(corpo), Charsets.UTF_8)
            Json.parseToJsonElement(json).jsonObject
        }.getOrNull()
    }
}

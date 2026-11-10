package com.pingo.core.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

internal object JwtSubject {
    fun de(token: String): String? {
        val corpo = token.split('.').getOrNull(1) ?: return null

        return runCatching {
            val json = String(Base64.getUrlDecoder().decode(corpo), Charsets.UTF_8)
            Json.parseToJsonElement(json).jsonObject["sub"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }
}

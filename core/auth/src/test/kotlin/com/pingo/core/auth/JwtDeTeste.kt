package com.pingo.core.auth

import java.util.Base64

internal fun jwtComPayload(payload: String): String {
    val codificador = Base64.getUrlEncoder().withoutPadding()
    val cabecalho = codificador.encodeToString("""{"alg":"RS256"}""".toByteArray())

    return listOf(cabecalho, codificador.encodeToString(payload.toByteArray()), "assinatura").joinToString(".")
}

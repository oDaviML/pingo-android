package com.pingo.core.data.categoria

import java.util.UUID

private const val SEPARADOR = "\u001F"

internal fun identificadorDeterministico(vararg partes: String): String =
    UUID.nameUUIDFromBytes(partes.joinToString(SEPARADOR).toByteArray(Charsets.UTF_8)).toString()

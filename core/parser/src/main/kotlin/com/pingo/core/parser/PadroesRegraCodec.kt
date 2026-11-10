package com.pingo.core.parser

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object PadroesRegraCodec {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializador = ListSerializer(PadraoRegra.serializer())

    fun decodificar(padroes: String): List<PadraoRegra> = json.decodeFromString(serializador, padroes)

    fun codificar(padroes: List<PadraoRegra>): String = json.encodeToString(serializador, padroes)
}

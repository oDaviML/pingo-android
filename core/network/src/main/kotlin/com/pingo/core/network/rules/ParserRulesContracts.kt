package com.pingo.core.network.rules

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class ParserRulesResponse(val versao: Int, val regras: List<ParserRuleResponse>)

@Serializable
data class ParserRuleResponse(
    val publicId: String,
    val instituicao: String,
    val versao: Int,
    val publicadaEm: String,
    val padroes: JsonArray,
)

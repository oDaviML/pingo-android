package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

object RegrasParserSeed {
    private const val RECURSO = "/regras/regras-v2.json"
    const val VERSAO = 2

    val regras: List<RegraParser> by lazy { Json.parseToJsonElement(recurso()).jsonArray.map(::regraDe) }

    private fun regraDe(elemento: JsonElement): RegraParser {
        val objeto = elemento.jsonObject
        val instituicao = InstituicaoFinanceira.valueOf(objeto.getValue("instituicao").jsonPrimitive.content)
        return RegraParser(
            publicId = "seed-${instituicao.name.lowercase()}-v$VERSAO",
            instituicao = instituicao,
            versao = VERSAO,
            padroes = objeto.getValue("padroes").toString(),
            ativa = true,
            publicadaEm = Instant.EPOCH,
            criadoEm = Instant.EPOCH,
            atualizadoEm = Instant.EPOCH,
        )
    }

    private fun recurso(): String =
        requireNotNull(RegrasParserSeed::class.java.getResource(RECURSO)) { "seed de regras ausente: $RECURSO" }.readText()
}

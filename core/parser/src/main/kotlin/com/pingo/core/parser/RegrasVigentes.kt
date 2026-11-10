package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.RegraParser

object RegrasVigentes {
    fun selecionar(regras: List<RegraParser>): Map<InstituicaoFinanceira, VersaoRegras> = regras
        .filter(RegraParser::ativa)
        .sortedByDescending(RegraParser::versao)
        .groupBy(RegraParser::instituicao)
        .mapNotNull { (instituicao, versoes) ->
            versoes.firstNotNullOfOrNull(::compilarSeValida)?.let { instituicao to it }
        }
        .toMap()

    private fun compilarSeValida(regra: RegraParser): VersaoRegras? = try {
        VersaoRegras.compilar(regra)
    } catch (invalida: IllegalArgumentException) {
        null
    }
}

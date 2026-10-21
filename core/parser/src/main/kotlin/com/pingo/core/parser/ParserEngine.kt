package com.pingo.core.parser

import com.pingo.core.model.Lancamento
import com.pingo.core.model.RegraParser

data class ParseOutcome(val lancamento: Lancamento?, val confianca: Double, val isTransferencia: Boolean)

interface ParserEngine {
    fun parse(rawText: String, rules: List<RegraParser>): ParseOutcome
}

package com.pingo.core.data.reconciliacao

import com.pingo.core.model.Instrumento
import java.time.Instant

interface CoordenadorDeContaFinanceiro {
    suspend fun aoCriarInstrumentos(instrumentos: List<Instrumento>, agora: Instant)

    suspend fun aoSalvarInstrumentos(alteracoes: List<Pair<Instrumento?, Instrumento>>, agora: Instant)

    object NoOp : CoordenadorDeContaFinanceiro {
        override suspend fun aoCriarInstrumentos(instrumentos: List<Instrumento>, agora: Instant) = Unit

        override suspend fun aoSalvarInstrumentos(alteracoes: List<Pair<Instrumento?, Instrumento>>, agora: Instant) = Unit
    }
}

package com.pingo.core.data.captura

import com.pingo.core.model.Lancamento

sealed interface ResultadoProcessamento {
    data class Gravado(val lancamento: Lancamento) : ResultadoProcessamento

    data object Duplicada : ResultadoProcessamento
}

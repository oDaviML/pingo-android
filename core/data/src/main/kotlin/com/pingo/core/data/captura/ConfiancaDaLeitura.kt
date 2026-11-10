package com.pingo.core.data.captura

import com.pingo.core.model.Categoria
import kotlin.math.roundToInt

internal object ConfiancaDaLeitura {
    private const val PESO_VALOR = 0.4
    private const val PESO_TIPO = 0.2
    private const val PESO_ESTABELECIMENTO = 0.2
    private const val PESO_CATEGORIA = 0.2

    fun calcular(valorCentavos: Long, estabelecimento: String?, categoria: Categoria?): Double {
        var confianca = PESO_TIPO

        if (valorCentavos > 0) {
            confianca += PESO_VALOR
        }
        if (!estabelecimento.isNullOrBlank()) {
            confianca += PESO_ESTABELECIMENTO
        }
        if (categoria != null) {
            confianca += PESO_CATEGORIA
        }

        return (confianca * 100).roundToInt() / 100.0
    }
}

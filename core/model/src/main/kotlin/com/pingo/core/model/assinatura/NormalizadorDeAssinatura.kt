package com.pingo.core.model.assinatura

import java.text.Normalizer
import java.util.Locale

object NormalizadorDeAssinatura {
    private val MARCAS_DIACRITICAS = Regex("\\p{M}+")
    private val ESPACOS = Regex("[\\s\\p{Zs}]+")

    fun normalizar(estabelecimento: String?): String? {
        if (estabelecimento == null) {
            return null
        }
        val decomposto = Normalizer.normalize(estabelecimento, Normalizer.Form.NFD)
        return MARCAS_DIACRITICAS.replace(decomposto, "")
            .lowercase(Locale.ROOT)
            .replace(ESPACOS, " ")
            .trim()
            .takeIf { it.isNotEmpty() }
    }
}

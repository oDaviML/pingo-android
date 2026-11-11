package com.pingo.feature.home.impl

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object RotuloDeMes {
    private val LOCALE: Locale = Locale.forLanguageTag("pt-BR")
    private val FORMATO: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE)

    fun de(mes: YearMonth): String = FORMATO.format(mes).replaceFirstChar { primeiro ->
        if (primeiro.isLowerCase()) primeiro.titlecase(LOCALE) else primeiro.toString()
    }

    fun emMaiusculas(mes: YearMonth): String = de(mes).uppercase(LOCALE)
}

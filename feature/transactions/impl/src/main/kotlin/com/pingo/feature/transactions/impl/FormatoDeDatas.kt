package com.pingo.feature.transactions.impl

import com.pingo.core.model.TipoLancamento
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE = Locale("pt", "BR")

object RotuloDeDia {
    private val FORMATO: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM", LOCALE)

    fun de(data: LocalDate, hoje: LocalDate): String = when (data) {
        hoje -> "HOJE"
        hoje.minusDays(1) -> "ONTEM"
        else -> data.format(FORMATO).uppercase(LOCALE)
    }
}

object RotuloDeMes {
    private val FORMATO: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE)

    fun de(mes: YearMonth): String = mes.format(FORMATO).replaceFirstChar { letra -> letra.titlecase(LOCALE) }
}

fun rotuloDoTipo(tipo: TipoLancamento?): String? = when (tipo) {
    TipoLancamento.DESPESA -> "Despesas"
    TipoLancamento.RECEITA -> "Receitas"
    TipoLancamento.TRANSFERENCIA -> "Transferências"
    TipoLancamento.AJUSTE -> "Ajustes"
    null -> null
}

fun rotuloDoStatus(status: StatusFiltro): String = when (status) {
    StatusFiltro.PENDENTE -> "Pendentes"
    StatusFiltro.CONFIRMADO -> "Confirmados"
    StatusFiltro.MANUAL -> "Manuais"
}

fun pluralDeFiltros(quantidade: Int): String = if (quantidade == 1) "1 filtro ativo" else "$quantidade filtros ativos"

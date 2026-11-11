package com.pingo.core.ui

import androidx.compose.ui.graphics.Color
import com.pingo.core.designsystem.theme.PingoColors
import com.pingo.core.model.TipoLancamento
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Valores {
    private val HORARIO: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR"))

    fun formatar(valorCentavos: Long): String = "R$ ${formatarDecimal(valorCentavos)}"

    fun assinado(valorCentavos: Long, tipo: TipoLancamento): String = when (tipo) {
        TipoLancamento.DESPESA -> "- ${formatar(valorCentavos)}"
        TipoLancamento.RECEITA -> "+ ${formatar(valorCentavos)}"
        TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> formatar(valorCentavos)
    }

    fun total(valorCentavos: Long): String = when {
        valorCentavos < 0 -> "- ${formatar(-valorCentavos)}"
        valorCentavos > 0 -> "+ ${formatar(valorCentavos)}"
        else -> formatar(0)
    }

    fun corDoTipo(tipo: TipoLancamento, cores: PingoColors): Color = when (tipo) {
        TipoLancamento.DESPESA -> cores.expense
        TipoLancamento.RECEITA -> cores.income
        TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> cores.transfer
    }

    fun horario(dataHora: Instant, zona: ZoneId): String = HORARIO.format(dataHora.atZone(zona))

    private fun formatarDecimal(valorCentavos: Long): String {
        val absoluto = kotlin.math.abs(valorCentavos)
        val reais = absoluto / 100
        val centavos = (absoluto % 100).toString().padStart(2, '0')
        val inteiro = reais.toString()
            .reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()

        return "$inteiro,$centavos"
    }
}

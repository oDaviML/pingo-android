package com.pingo.core.parser

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object DataHoraDoLancamento {
    private val formato = Regex("""(\d{1,2})/(\d{1,2})(?:/(\d{4}))?(?:\s+às\s+(\d{1,2}):(\d{2}))?""")

    fun resolver(texto: String?, referencia: Instant, fuso: ZoneId): Instant {
        val referenciaLocal = referencia.atZone(fuso)
        val informada = texto
            ?.let { valor -> formato.find(valor) }
            ?.let { achado -> dataDe(achado, referenciaLocal.year) }
            ?: return referencia
        val limite = referenciaLocal.toLocalDate().plusDays(TOLERANCIA_EM_DIAS)

        val candidata = if (informada.anoInformado || !informada.dataHora.toLocalDate().isAfter(limite)) {
            informada.dataHora
        } else {
            informada.dataHora.minusYears(1)
        }

        return candidata.atZone(fuso).toInstant()
    }

    private fun dataDe(achado: MatchResult, anoDeReferencia: Int): DataInformada? {
        val dia = achado.groupValues[1].toIntOrNull()
        val mes = achado.groupValues[2].toIntOrNull()
        val ano = achado.groupValues[3].toIntOrNull()
        val hora = achado.groupValues[4].toIntOrNull() ?: 0
        val minuto = achado.groupValues[5].toIntOrNull() ?: 0
        val dataHora = if (dia == null || mes == null) {
            null
        } else {
            runCatching { LocalDateTime.of(ano ?: anoDeReferencia, mes, dia, hora, minuto) }.getOrNull()
        }

        return dataHora?.let { DataInformada(it, ano != null) }
    }

    private data class DataInformada(val dataHora: LocalDateTime, val anoInformado: Boolean)

    private const val TOLERANCIA_EM_DIAS = 1L
}

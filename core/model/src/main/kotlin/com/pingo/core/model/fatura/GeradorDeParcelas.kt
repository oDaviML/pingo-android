package com.pingo.core.model.fatura

import com.pingo.core.model.Instrumento
import com.pingo.core.model.diaDoMesInformado
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

enum class ModoParcelamento {
    VALOR_TOTAL,
    VALOR_PARCELA,
}

data class ParcelaPlanejada(
    val numero: Int,
    val totalParcelas: Int,
    val valorCentavos: Long,
    val dataHora: Instant,
    val valorTotalCompraCentavos: Long?,
)

object GeradorDeParcelas {
    const val MIN_PARCELAS = 1
    const val MAX_PARCELAS = 99

    @Suppress("LongParameterList")
    fun planejar(
        valorBaseCentavos: Long,
        totalParcelas: Int,
        modo: ModoParcelamento,
        dataHoraInicial: Instant,
        instrumento: Instrumento,
        vigencias: List<VigenciaDeCartao> = emptyList(),
        zona: ZoneId = CalendarioDeFatura.ZONA_SAO_PAULO,
    ): List<ParcelaPlanejada> {
        require(totalParcelas in MIN_PARCELAS..MAX_PARCELAS) { "Parcelas fora da faixa: $totalParcelas" }
        require(valorBaseCentavos > 0L) { "Valor base deve ser positivo" }

        if (totalParcelas == 1) {
            return listOf(
                ParcelaPlanejada(
                    numero = 1,
                    totalParcelas = 1,
                    valorCentavos = valorBaseCentavos,
                    dataHora = dataHoraInicial,
                    valorTotalCompraCentavos = null,
                ),
            )
        }

        val valores = dividirValores(valorBaseCentavos, totalParcelas, modo)
        val valorTotal = valores.sum()
        val zonedInicial = ZonedDateTime.ofInstant(dataHoraInicial, zona)
        val cicloInicial = CalendarioDeFatura.cicloParaCompra(instrumento, dataHoraInicial, vigencias)

        return valores.mapIndexed { indice, valorParcela ->
            planejarParcela(
                indice = indice,
                totalParcelas = totalParcelas,
                valorParcela = valorParcela,
                valorTotal = valorTotal,
                dataHoraInicial = dataHoraInicial,
                zonedInicial = zonedInicial,
                cicloInicial = cicloInicial,
                instrumento = instrumento,
                vigencias = vigencias,
                zona = zona,
            )
        }
    }

    @Suppress("LongParameterList")
    private fun planejarParcela(
        indice: Int,
        totalParcelas: Int,
        valorParcela: Long,
        valorTotal: Long,
        dataHoraInicial: Instant,
        zonedInicial: ZonedDateTime,
        cicloInicial: ResultadoCalculoCiclo,
        instrumento: Instrumento,
        vigencias: List<VigenciaDeCartao>,
        zona: ZoneId,
    ): ParcelaPlanejada {
        val numero = indice + 1
        val dataHora = if (indice == 0) {
            dataHoraInicial
        } else {
            instanteDaParcela(
                dataInicial = zonedInicial.toLocalDate(),
                horario = zonedInicial.toLocalTime(),
                mesesAdicionais = indice.toLong(),
                cicloInicial = cicloInicial,
                instrumento = instrumento,
                vigencias = vigencias,
                zona = zona,
            )
        }
        return ParcelaPlanejada(
            numero = numero,
            totalParcelas = totalParcelas,
            valorCentavos = valorParcela,
            dataHora = dataHora,
            valorTotalCompraCentavos = valorTotal.takeIf { numero == 1 },
        )
    }

    fun dividirValores(valorBaseCentavos: Long, totalParcelas: Int, modo: ModoParcelamento): List<Long> = when (modo) {
        ModoParcelamento.VALOR_TOTAL -> {
            require(valorBaseCentavos >= totalParcelas) { "Valor total menor que o número de parcelas" }
            val base = valorBaseCentavos / totalParcelas
            val resto = (valorBaseCentavos % totalParcelas).toInt()
            List(totalParcelas) { indice -> base + if (indice < resto) 1L else 0L }
        }
        ModoParcelamento.VALOR_PARCELA -> List(totalParcelas) { valorBaseCentavos }
    }

    @Suppress("LongParameterList")
    private fun instanteDaParcela(
        dataInicial: LocalDate,
        horario: LocalTime,
        mesesAdicionais: Long,
        cicloInicial: ResultadoCalculoCiclo,
        instrumento: Instrumento,
        vigencias: List<VigenciaDeCartao>,
        zona: ZoneId,
    ): Instant {
        val dataCandidata = dataInicial.plusMonths(mesesAdicionais)
        val instanteCandidato = dataCandidata.atTime(horario).atZone(zona).toInstant()
        val cicloAlvo = (cicloInicial as? ResultadoCalculoCiclo.CicloCalculado)?.let {
            YearMonth.from(it.ciclo.competencia).plusMonths(mesesAdicionais)
        }
        val cicloCalculado = CalendarioDeFatura.cicloParaCompra(instrumento, instanteCandidato, vigencias)
            as? ResultadoCalculoCiclo.CicloCalculado
        val competenciaCalculada = cicloCalculado?.let { YearMonth.from(it.ciclo.competencia) }
        val precisaAjustar = cicloAlvo != null && competenciaCalculada != null && competenciaCalculada != cicloAlvo

        return if (!precisaAjustar) {
            instanteCandidato
        } else {
            val dataAjustada = ajustarDataParaCompetencia(
                competenciaAlvo = cicloAlvo!!,
                competenciaCalculada = competenciaCalculada!!,
                fechamentoDia = fechamentoEfetivo(instrumento, instanteCandidato, vigencias),
            )
            (dataAjustada ?: dataCandidata).atTime(horario).atZone(zona).toInstant()
        }
    }

    private fun ajustarDataParaCompetencia(competenciaAlvo: YearMonth, competenciaCalculada: YearMonth, fechamentoDia: Int?): LocalDate? {
        if (!diaDoMesInformado(fechamentoDia)) {
            return null
        }
        val fechamento = requireNotNull(fechamentoDia)
        return if (competenciaCalculada < competenciaAlvo) {
            CalendarioDeFatura.diaAjustadoNoMes(competenciaAlvo.minusMonths(1), fechamento)
        } else {
            CalendarioDeFatura.diaAjustadoNoMes(competenciaAlvo, fechamento).minusDays(1)
        }
    }

    private fun fechamentoEfetivo(instrumento: Instrumento, instante: Instant, vigencias: List<VigenciaDeCartao>): Int? {
        val vigencia = CalendarioDeFatura.resolverVigencia(instrumento.publicId, instante, vigencias)
        return vigencia?.fechamentoDia ?: instrumento.fechamentoDia
    }
}

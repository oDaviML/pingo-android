package com.pingo.core.model.fatura

import com.pingo.core.model.Fatura
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.diaDoMesInformado
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

object CalendarioDeFatura {
    val ZONA_SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")

    fun diaAjustadoNoMes(mes: YearMonth, diaConfigurado: Int): LocalDate = mes.atDay(minOf(diaConfigurado, mes.lengthOfMonth()))

    fun competenciaDe(dataFechamento: LocalDate): LocalDate = dataFechamento.withDayOfMonth(1)

    fun calcularVencimento(dataFechamento: LocalDate, vencimentoDia: Int): LocalDate {
        val mesFechamento = YearMonth.from(dataFechamento)
        val candidatoNoMes = diaAjustadoNoMes(mesFechamento, vencimentoDia)
        return if (candidatoNoMes.isAfter(dataFechamento)) {
            candidatoNoMes
        } else {
            diaAjustadoNoMes(mesFechamento.plusMonths(1), vencimentoDia)
        }
    }

    fun cicloDaCompetencia(
        competencia: LocalDate,
        fechamentoDia: Int?,
        vencimentoDia: Int?,
        vigenciaPublicId: String? = null,
    ): ResultadoCalculoCiclo {
        if (!diaDoMesInformado(fechamentoDia) || !diaDoMesInformado(vencimentoDia)) {
            return ResultadoCalculoCiclo.ConfiguracaoIncompleta
        }
        val fechamento = requireNotNull(fechamentoDia)
        val vencimento = requireNotNull(vencimentoDia)
        val mesCompetencia = YearMonth.from(competencia)
        val dataFechamento = diaAjustadoNoMes(mesCompetencia, fechamento)
        val dataFechamentoAnterior = diaAjustadoNoMes(mesCompetencia.minusMonths(1), fechamento)
        val dataVencimento = calcularVencimento(dataFechamento, vencimento)
        return ResultadoCalculoCiclo.CicloCalculado(
            ciclo = CicloDeFatura(
                competencia = mesCompetencia.atDay(1),
                dataFechamento = dataFechamento,
                dataVencimento = dataVencimento,
                inicioInclusivo = dataFechamentoAnterior.atStartOfDay(ZONA_SAO_PAULO).toInstant(),
                fimExclusivo = dataFechamento.atStartOfDay(ZONA_SAO_PAULO).toInstant(),
                fechamentoDia = fechamento,
                vencimentoDia = vencimento,
            ),
            vigenciaPublicId = vigenciaPublicId,
        )
    }

    fun cicloParaInstante(
        dataHora: Instant,
        fechamentoDia: Int?,
        vencimentoDia: Int?,
        vigenciaPublicId: String? = null,
    ): ResultadoCalculoCiclo {
        if (!diaDoMesInformado(fechamentoDia) || !diaDoMesInformado(vencimentoDia)) {
            return ResultadoCalculoCiclo.ConfiguracaoIncompleta
        }
        val fechamento = requireNotNull(fechamentoDia)
        val dataLocal = ZonedDateTime.ofInstant(dataHora, ZONA_SAO_PAULO).toLocalDate()
        val mesDaData = YearMonth.from(dataLocal)
        val fechamentoNoMesDaData = diaAjustadoNoMes(mesDaData, fechamento)
        val mesCompetencia = if (dataLocal.isBefore(fechamentoNoMesDaData)) {
            mesDaData
        } else {
            mesDaData.plusMonths(1)
        }
        return cicloDaCompetencia(
            competencia = mesCompetencia.atDay(1),
            fechamentoDia = fechamento,
            vencimentoDia = vencimentoDia,
            vigenciaPublicId = vigenciaPublicId,
        )
    }

    fun resolverVigencia(instrumentoPublicId: String, dataHora: Instant, vigencias: List<VigenciaDeCartao>): VigenciaDeCartao? {
        val ordenadas = vigencias
            .filter { it.instrumentoPublicId == instrumentoPublicId && it.excluidoEm == null }
            .sortedWith(compareBy<VigenciaDeCartao> { it.vigenciaInicio }.thenBy { it.publicId })
        val aplicavel = ordenadas.lastOrNull { !it.vigenciaInicio.isAfter(dataHora) }
        return aplicavel ?: ordenadas.firstOrNull()
    }

    fun cicloParaCompra(
        instrumento: Instrumento,
        dataHora: Instant,
        vigencias: List<VigenciaDeCartao> = emptyList(),
    ): ResultadoCalculoCiclo {
        if (instrumento.tipo != TipoInstrumento.CREDITO) {
            return ResultadoCalculoCiclo.ConfiguracaoIncompleta
        }
        val vigencia = resolverVigencia(instrumento.publicId, dataHora, vigencias)
        val fechamentoDia = if (vigencia != null) vigencia.fechamentoDia else instrumento.fechamentoDia
        val vencimentoDia = if (vigencia != null) vigencia.vencimentoDia else instrumento.vencimentoDia
        return cicloParaInstante(
            dataHora = dataHora,
            fechamentoDia = fechamentoDia,
            vencimentoDia = vencimentoDia,
            vigenciaPublicId = vigencia?.publicId,
        )
    }

    fun resolverFaturaDaCompra(
        instrumento: Instrumento,
        dataHora: Instant,
        vigencias: List<VigenciaDeCartao>,
        faturasExistentes: List<Fatura>,
    ): ResultadoResolucaoFatura {
        val resultadoCiclo = cicloParaCompra(instrumento, dataHora, vigencias)
        return when (resultadoCiclo) {
            ResultadoCalculoCiclo.ConfiguracaoIncompleta -> ResultadoResolucaoFatura.ConfiguracaoIncompleta
            is ResultadoCalculoCiclo.CicloCalculado -> {
                val existente = faturasExistentes.firstOrNull {
                    it.excluidoEm == null &&
                        it.instrumentoPublicId == instrumento.publicId &&
                        it.competencia == resultadoCiclo.ciclo.competencia
                }
                if (existente != null) {
                    ResultadoResolucaoFatura.FaturaExistente(
                        fatura = existente,
                        vigenciaPublicId = resultadoCiclo.vigenciaPublicId,
                    )
                } else {
                    ResultadoResolucaoFatura.NovoCiclo(
                        ciclo = resultadoCiclo.ciclo,
                        vigenciaPublicId = resultadoCiclo.vigenciaPublicId,
                    )
                }
            }
        }
    }

    fun statusDoCiclo(dataFechamento: LocalDate, dataReferenciaLocal: LocalDate, paga: Boolean = false): StatusFatura = when {
        paga -> StatusFatura.PAGA
        dataReferenciaLocal.isBefore(dataFechamento) -> StatusFatura.ABERTA
        else -> StatusFatura.FECHADA
    }

    fun entraNaFatura(lancamento: Lancamento): Boolean = lancamento.excluidoEm == null &&
        lancamento.status == StatusLancamento.CONFIRMADO &&
        lancamento.tipo == TipoLancamento.DESPESA

    fun ehPagamentoOuTransferencia(lancamento: Lancamento): Boolean =
        lancamento.excluidoEm == null && lancamento.tipo == TipoLancamento.TRANSFERENCIA

    fun calcularTotalFaturaCentavos(lancamentos: List<Lancamento>, faturaPublicId: String? = null): Long = lancamentos
        .asSequence()
        .filter { entraNaFatura(it) }
        .filter { faturaPublicId == null || it.faturaPublicId == faturaPublicId }
        .sumOf { it.valorCentavos }
}

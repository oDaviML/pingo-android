package com.pingo.core.data.impl

import com.pingo.core.model.Fatura
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.diaDoMesInformado
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.model.fatura.CicloDeFatura
import com.pingo.core.model.fatura.CicloDeFaturaDetalhado
import com.pingo.core.model.fatura.EstadoFaturaCartao
import com.pingo.core.model.fatura.ResultadoCalculoCiclo
import com.pingo.core.model.fatura.TipoCicloFatura
import com.pingo.core.model.fatura.VigenciaDeCartao
import com.pingo.core.model.voucher.CalculadoraDeSaldoVoucher
import com.pingo.core.model.voucher.HistoricoAjusteVoucher
import com.pingo.core.model.voucher.RecargaProgramada
import com.pingo.core.model.voucher.ReferenciaSaldoVoucher
import com.pingo.core.model.voucher.StatusRecargaProgramada
import com.pingo.core.model.voucher.TipoReferenciaSaldoVoucher
import com.pingo.core.model.voucher.UltimaRecargaVoucher
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

internal object CalculadorDeIndicadoresFinanceiros {
    fun calcularEstadoFaturaAberta(
        instrumento: Instrumento,
        agora: Instant,
        hoje: LocalDate,
        vigencias: List<VigenciaDeCartao>,
        faturas: List<Fatura>,
        lancamentos: List<Lancamento>,
    ): EstadoFaturaCartao {
        val cicloAtual = resolverCicloAberto(instrumento, agora, hoje, vigencias)
        val faturaDoCiclo = cicloAtual?.let { c -> faturas.firstOrNull { it.competencia == c.competencia } }
        return when {
            cicloAtual == null -> estadoParaConfiguracaoIncompleta(hoje, faturas, lancamentos)
            faturaDoCiclo != null && hoje.isBefore(faturaDoCiclo.dataFechamento) ->
                configurarEstadoDeFatura(faturaDoCiclo, hoje, lancamentos)
            faturaDoCiclo != null ->
                estadoQuandoCicloFechou(
                    contexto = ContextoEstadoFatura(instrumento, agora, hoje, vigencias, faturas, lancamentos),
                    faturaDoCiclo = faturaDoCiclo,
                    cicloAtual = cicloAtual,
                )
            else -> estadoVazioDeCiclo(cicloAtual, hoje)
        }
    }

    fun montarCicloExistente(
        instrumentoPublicId: String,
        fatura: Fatura,
        hoje: LocalDate,
        competenciaAberta: LocalDate?,
        lancamentos: List<Lancamento>,
    ): CicloDeFaturaDetalhado {
        val status = CalendarioDeFatura.statusDoCiclo(fatura.dataFechamento, hoje, fatura.status == StatusFatura.PAGA)
        val itens = lancamentos
            .filter { it.faturaPublicId == fatura.publicId && CalendarioDeFatura.entraNaFatura(it) }
            .sortedByDescending { it.dataHora }
        val total = CalendarioDeFatura.calcularTotalFaturaCentavos(itens, fatura.publicId)
        val dias = ChronoUnit.DAYS.between(hoje, fatura.dataVencimento)
        val tipoCiclo = when {
            status == StatusFatura.FECHADA || status == StatusFatura.PAGA -> TipoCicloFatura.FECHADA
            competenciaAberta == null || fatura.competencia == competenciaAberta -> TipoCicloFatura.ABERTA
            else -> TipoCicloFatura.PROXIMA
        }
        return CicloDeFaturaDetalhado(
            faturaPublicId = fatura.publicId,
            instrumentoPublicId = instrumentoPublicId,
            competencia = fatura.competencia,
            dataFechamento = fatura.dataFechamento,
            dataVencimento = fatura.dataVencimento,
            totalCentavos = total,
            status = status,
            tipoCiclo = tipoCiclo,
            diasParaVencimento = dias,
            vencida = dias < 0 && status != StatusFatura.PAGA,
            lancamentos = itens,
        )
    }

    fun montarCicloAbertoVazio(instrumentoPublicId: String, estadoAberto: EstadoFaturaCartao.Configurada): CicloDeFaturaDetalhado =
        CicloDeFaturaDetalhado(
            faturaPublicId = null,
            instrumentoPublicId = instrumentoPublicId,
            competencia = estadoAberto.competencia,
            dataFechamento = estadoAberto.dataFechamento,
            dataVencimento = estadoAberto.dataVencimento,
            totalCentavos = 0L,
            status = StatusFatura.ABERTA,
            tipoCiclo = TipoCicloFatura.ABERTA,
            diasParaVencimento = estadoAberto.diasParaVencimento,
            vencida = estadoAberto.vencida,
            lancamentos = emptyList(),
        )

    fun calcularUltimaRecarga(recargas: List<RecargaProgramada>, lancamentos: List<Lancamento>): UltimaRecargaVoucher? {
        val efetivadasIds = recargas
            .asSequence()
            .filter { it.status == StatusRecargaProgramada.EFETIVADA && it.lancamentoPublicId != null }
            .mapNotNull { it.lancamentoPublicId }
            .toSet()
        val ultimaReceita = lancamentos
            .asSequence()
            .filter { it.excluidoEm == null && it.status == StatusLancamento.CONFIRMADO && it.tipo == TipoLancamento.RECEITA }
            .maxWithOrNull(compareBy<Lancamento> { it.dataHora }.thenBy { it.publicId })
            ?: return null
        val dataLocal = ZonedDateTime.ofInstant(ultimaReceita.dataHora, CalculadoraDeSaldoVoucher.ZONA_SAO_PAULO).toLocalDate()
        return UltimaRecargaVoucher(
            data = dataLocal,
            valorCentavos = ultimaReceita.valorCentavos,
            prevista = ultimaReceita.publicId in efetivadasIds,
        )
    }

    fun montarHistoricoAjustes(referencias: List<ReferenciaSaldoVoucher>, lancamentos: List<Lancamento>): List<HistoricoAjusteVoucher> {
        val lancamentosPorId = lancamentos.associateBy { it.publicId }
        return referencias
            .asSequence()
            .filter { it.tipo == TipoReferenciaSaldoVoucher.AJUSTE && it.excluidoEm == null }
            .sortedByDescending { it.instanteReferencia }
            .map { ref ->
                HistoricoAjusteVoucher(
                    referenciaPublicId = ref.publicId,
                    lancamentoPublicId = ref.lancamentoPublicId ?: ref.publicId,
                    saldoAnteriorCentavos = ref.saldoCentavos - ref.deltaCentavos,
                    saldoAlvoCentavos = ref.saldoCentavos,
                    deltaCentavos = ref.deltaCentavos,
                    instante = ref.instanteReferencia,
                    observacao = ref.lancamentoPublicId?.let { lancamentosPorId[it]?.observacao },
                )
            }
            .toList()
    }

    private fun estadoParaConfiguracaoIncompleta(
        hoje: LocalDate,
        faturas: List<Fatura>,
        lancamentos: List<Lancamento>,
    ): EstadoFaturaCartao {
        val abertaExistente = faturas
            .filter { CalendarioDeFatura.statusDoCiclo(it.dataFechamento, hoje, it.status == StatusFatura.PAGA) == StatusFatura.ABERTA }
            .minByOrNull { it.competencia }
        return if (abertaExistente != null) {
            configurarEstadoDeFatura(abertaExistente, hoje, lancamentos)
        } else {
            EstadoFaturaCartao.ConfiguracaoIncompleta
        }
    }

    private fun estadoQuandoCicloFechou(
        contexto: ContextoEstadoFatura,
        faturaDoCiclo: Fatura,
        cicloAtual: CicloDeFatura,
    ): EstadoFaturaCartao {
        val proximaCompetencia = faturaDoCiclo.competencia.plusMonths(1)
        val faturaProxima = contexto.faturas.firstOrNull { it.competencia == proximaCompetencia }
        if (faturaProxima != null) {
            return configurarEstadoDeFatura(faturaProxima, contexto.hoje, contexto.lancamentos)
        }
        val vigencia = CalendarioDeFatura.resolverVigencia(contexto.instrumento.publicId, contexto.agora, contexto.vigencias)
        val proximoCiclo = CalendarioDeFatura.cicloDaCompetencia(
            competencia = proximaCompetencia,
            fechamentoDia = vigencia?.fechamentoDia ?: contexto.instrumento.fechamentoDia,
            vencimentoDia = vigencia?.vencimentoDia ?: contexto.instrumento.vencimentoDia,
        )
        val cicloAlvo = (proximoCiclo as? ResultadoCalculoCiclo.CicloCalculado)?.ciclo ?: cicloAtual
        return estadoVazioDeCiclo(cicloAlvo, contexto.hoje)
    }

    private data class ContextoEstadoFatura(
        val instrumento: Instrumento,
        val agora: Instant,
        val hoje: LocalDate,
        val vigencias: List<VigenciaDeCartao>,
        val faturas: List<Fatura>,
        val lancamentos: List<Lancamento>,
    )

    private fun resolverCicloAberto(
        instrumento: Instrumento,
        agora: Instant,
        hoje: LocalDate,
        vigencias: List<VigenciaDeCartao>,
    ): CicloDeFatura? {
        val vigencia = CalendarioDeFatura.resolverVigencia(instrumento.publicId, agora, vigencias)
        val fechamento = vigencia?.fechamentoDia ?: instrumento.fechamentoDia
        val vencimento = vigencia?.vencimentoDia ?: instrumento.vencimentoDia
        if (!diaDoMesInformado(fechamento) || !diaDoMesInformado(vencimento)) {
            return null
        }
        val calculo = CalendarioDeFatura.cicloParaInstante(
            dataHora = hoje.atTime(12, 0).atZone(CalendarioDeFatura.ZONA_SAO_PAULO).toInstant(),
            fechamentoDia = fechamento,
            vencimentoDia = vencimento,
        )
        return (calculo as? ResultadoCalculoCiclo.CicloCalculado)?.ciclo
    }

    private fun configurarEstadoDeFatura(fatura: Fatura, hoje: LocalDate, lancamentos: List<Lancamento>): EstadoFaturaCartao.Configurada {
        val status = CalendarioDeFatura.statusDoCiclo(fatura.dataFechamento, hoje, fatura.status == StatusFatura.PAGA)
        val dias = ChronoUnit.DAYS.between(hoje, fatura.dataVencimento)
        val total = CalendarioDeFatura.calcularTotalFaturaCentavos(lancamentos, fatura.publicId)
        return EstadoFaturaCartao.Configurada(
            faturaPublicId = fatura.publicId,
            competencia = fatura.competencia,
            dataFechamento = fatura.dataFechamento,
            dataVencimento = fatura.dataVencimento,
            totalCentavos = total,
            status = status,
            diasParaVencimento = dias,
            vencida = dias < 0 && status != StatusFatura.PAGA,
        )
    }

    private fun estadoVazioDeCiclo(ciclo: CicloDeFatura, hoje: LocalDate): EstadoFaturaCartao.Configurada {
        val dias = ChronoUnit.DAYS.between(hoje, ciclo.dataVencimento)
        return EstadoFaturaCartao.Configurada(
            faturaPublicId = null,
            competencia = ciclo.competencia,
            dataFechamento = ciclo.dataFechamento,
            dataVencimento = ciclo.dataVencimento,
            totalCentavos = 0L,
            status = StatusFatura.ABERTA,
            diasParaVencimento = dias,
            vencida = dias < 0,
        )
    }
}

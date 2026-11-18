package com.pingo.core.data.impl

import com.pingo.core.data.mapper.toModel
import com.pingo.core.data.mapper.toVigencia
import com.pingo.core.data.projecao.ContasFinanceiroProjection
import com.pingo.core.data.voucher.MaterializarRecargasVencidasUseCase
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Conta
import com.pingo.core.model.Fatura
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.diaDoMesInformado
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.model.fatura.CicloDeFatura
import com.pingo.core.model.fatura.CicloDeFaturaDetalhado
import com.pingo.core.model.fatura.EstadoFaturaCartao
import com.pingo.core.model.fatura.FaturasDoCartao
import com.pingo.core.model.fatura.ResultadoCalculoCiclo
import com.pingo.core.model.fatura.TipoCicloFatura
import com.pingo.core.model.fatura.VigenciaDeCartao
import com.pingo.core.model.organizacao.ContaComIndicadores
import com.pingo.core.model.organizacao.IndicadorDeInstrumento
import com.pingo.core.model.organizacao.ResumoContasFinanceiro
import com.pingo.core.model.relatorio.AgregadorDeRelatorioMensal
import com.pingo.core.model.voucher.CalculadoraDeSaldoVoucher
import com.pingo.core.model.voucher.DetalheVoucher
import com.pingo.core.model.voucher.HistoricoAjusteVoucher
import com.pingo.core.model.voucher.RecargaProgramada
import com.pingo.core.model.voucher.ReferenciaSaldoVoucher
import com.pingo.core.model.voucher.StatusRecargaProgramada
import com.pingo.core.model.voucher.TipoReferenciaSaldoVoucher
import com.pingo.core.model.voucher.UltimaRecargaVoucher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContasFinanceiroProjectionImpl @Inject constructor(
    private val fontes: FontesContasProjection,
    private val materializarRecargasVencidas: MaterializarRecargasVencidasUseCase,
    private val clock: Clock,
) : ContasFinanceiroProjection {
    private val contaDao = fontes.contaDao
    private val instrumentoDao = fontes.instrumentoDao
    private val faturaDao = fontes.faturaDao
    private val configuracaoCartaoDao = fontes.configuracaoCartaoDao
    private val referenciaDao = fontes.referenciaDao
    private val recargaDao = fontes.recargaDao
    private val lancamentoDao = fontes.lancamentoDao
    override fun observeContas(): Flow<ResumoContasFinanceiro> {
        val fluxoCadastros = combine(
            contaDao.observeNaoExcluidas(),
            instrumentoDao.observeNaoExcluidos(),
            configuracaoCartaoDao.observeAtivas(),
            faturaDao.observeAtivas(),
        ) { contas, instrumentos, vigencias, faturas ->
            CadastrosBase(
                contas = contas.map { it.toModel() },
                instrumentos = instrumentos.map { it.toModel() },
                vigencias = vigencias.map { it.toVigencia() },
                faturas = faturas.map { it.toModel() },
            )
        }
        val fluxoMovimentos = combine(
            referenciaDao.observeAtivas(),
            recargaDao.observeAtivas(),
            lancamentoDao.observeAtivos(),
        ) { referencias, recargas, lancamentos ->
            Triple(referencias, recargas, lancamentos.map { it.toModel() })
        }
        return combine(fluxoCadastros, fluxoMovimentos) { base, (refs, recs, lancamentos) ->
            montarResumoContas(
                base = base,
                referencias = refs.map { it.toModel() },
                recargas = recs.map { rec ->
                    val inst = base.instrumentos.firstOrNull { it.publicId == rec.instrumentoPublicId }
                    rec.toModel(inst?.beneficio ?: BeneficioVoucher.VA)
                },
                lancamentos = lancamentos,
            )
        }.onStart {
            runCatching { materializarRecargasVencidas() }
        }
    }

    override fun observeFaturasDoCartao(instrumentoPublicId: String): Flow<FaturasDoCartao?> = combine(
        instrumentoDao.observeNaoExcluidos(),
        contaDao.observeNaoExcluidas(),
        configuracaoCartaoDao.observePorInstrumento(instrumentoPublicId),
        faturaDao.observePorInstrumento(instrumentoPublicId),
        lancamentoDao.observeAtivos(),
    ) { instrumentos, contas, vigencias, faturas, lancamentos ->
        val instrumento = instrumentos.firstOrNull { it.publicId == instrumentoPublicId }?.toModel()
            ?.takeIf { it.tipo == TipoInstrumento.CREDITO }
            ?: return@combine null
        val conta = contas.firstOrNull { it.publicId == instrumento.contaPublicId }?.toModel()
        montarFaturasDoCartao(
            instrumento = instrumento,
            conta = conta,
            vigencias = vigencias.map { it.toVigencia() },
            faturas = faturas.map { it.toModel() },
            lancamentos = lancamentos.map { it.toModel() }.filter { it.instrumentoPublicId == instrumentoPublicId },
        )
    }

    override fun observeDetalheVoucher(instrumentoPublicId: String): Flow<DetalheVoucher?> = combine(
        instrumentoDao.observeNaoExcluidos(),
        contaDao.observeNaoExcluidas(),
        referenciaDao.observePorInstrumento(instrumentoPublicId),
        recargaDao.observePorInstrumento(instrumentoPublicId),
        lancamentoDao.observeAtivos(),
    ) { instrumentos, contas, referencias, recargas, lancamentos ->
        val instrumento = instrumentos.firstOrNull { it.publicId == instrumentoPublicId }?.toModel()
            ?.takeIf { it.tipo == TipoInstrumento.VOUCHER }
            ?: return@combine null
        val conta = contas.firstOrNull { it.publicId == instrumento.contaPublicId }?.toModel()
        val refs = referencias.map { it.toModel() }
        val recs = recargas.map { it.toModel(instrumento.beneficio ?: BeneficioVoucher.VA) }
        val lancs = lancamentos.map { it.toModel() }.filter { it.instrumentoPublicId == instrumentoPublicId }
        DetalheVoucher(
            instrumento = instrumento,
            conta = conta,
            saldoAtualCentavos = CalculadoraDeSaldoVoucher.calcularSaldoCentavos(instrumento, refs, lancs),
            ultimaRecarga = calcularUltimaRecarga(recs, lancs),
            recargasProgramadas = recs,
            referencias = refs,
            historicoAjustes = montarHistoricoAjustes(refs, lancs),
        )
    }.onStart {
        runCatching { materializarRecargasVencidas() }
    }

    private fun montarResumoContas(
        base: CadastrosBase,
        referencias: List<ReferenciaSaldoVoucher>,
        recargas: List<RecargaProgramada>,
        lancamentos: List<Lancamento>,
    ): ResumoContasFinanceiro {
        val agora = clock.instant()
        val hoje = ZonedDateTime.ofInstant(agora, CalendarioDeFatura.ZONA_SAO_PAULO).toLocalDate()
        val instPorConta = base.instrumentos.groupBy { it.contaPublicId }
        val vigenciasPorInst = base.vigencias.groupBy { it.instrumentoPublicId }
        val faturasPorInst = base.faturas.groupBy { it.instrumentoPublicId }
        val refsPorInst = referencias.groupBy { it.instrumentoPublicId }
        val recsPorInst = recargas.groupBy { it.instrumentoPublicId }
        val lancsPorInst = lancamentos.filter { it.instrumentoPublicId != null }.groupBy { it.instrumentoPublicId!! }
        val mapearConta: (Conta) -> ContaComIndicadores = { conta ->
            val insts = instPorConta[conta.publicId].orEmpty()
                .filter { conta.arquivada || !it.arquivado }
                .map { inst ->
                    montarIndicador(
                        instrumento = inst,
                        hoje = hoje,
                        vigencias = vigenciasPorInst[inst.publicId].orEmpty(),
                        faturas = faturasPorInst[inst.publicId].orEmpty(),
                        referencias = refsPorInst[inst.publicId].orEmpty(),
                        recargas = recsPorInst[inst.publicId].orEmpty(),
                        lancamentos = lancsPorInst[inst.publicId].orEmpty(),
                    )
                }
            ContaComIndicadores(conta = conta, instrumentos = insts)
        }
        return ResumoContasFinanceiro(
            contasAtivas = base.contas.filter { !it.arquivada }.map(mapearConta),
            contasArquivadas = base.contas.filter { it.arquivada }.map(mapearConta),
        )
    }

    private fun montarIndicador(
        instrumento: Instrumento,
        hoje: LocalDate,
        vigencias: List<VigenciaDeCartao>,
        faturas: List<Fatura>,
        referencias: List<ReferenciaSaldoVoucher>,
        recargas: List<RecargaProgramada>,
        lancamentos: List<Lancamento>,
    ): IndicadorDeInstrumento = when (instrumento.tipo) {
        TipoInstrumento.CREDITO -> IndicadorDeInstrumento.Credito(
            instrumento = instrumento,
            estadoFatura = calcularEstadoFaturaAberta(instrumento, hoje, vigencias, faturas, lancamentos),
        )
        TipoInstrumento.DEBITO -> {
            val mes = YearMonth.from(hoje)
            val gasto = lancamentos
                .asSequence()
                .filter {
                    it.excluidoEm == null &&
                        it.status == StatusLancamento.CONFIRMADO &&
                        it.tipo == TipoLancamento.DESPESA &&
                        AgregadorDeRelatorioMensal.pertenceAoMes(it.dataHora, mes)
                }
                .sumOf { it.valorCentavos }
            IndicadorDeInstrumento.Debito(instrumento = instrumento, gastoDoMesCentavos = gasto, competenciaMes = mes)
        }
        TipoInstrumento.VOUCHER -> IndicadorDeInstrumento.Voucher(
            instrumento = instrumento,
            beneficio = instrumento.beneficio,
            saldoCentavos = CalculadoraDeSaldoVoucher.calcularSaldoCentavos(instrumento, referencias, lancamentos),
            ultimaRecarga = calcularUltimaRecarga(recargas, lancamentos),
        )
    }

    private fun calcularEstadoFaturaAberta(
        instrumento: Instrumento,
        hoje: LocalDate,
        vigencias: List<VigenciaDeCartao>,
        faturas: List<Fatura>,
        lancamentos: List<Lancamento>,
    ): EstadoFaturaCartao {
        val cicloAtual = resolverCicloAberto(instrumento, clock.instant(), hoje, vigencias)
        val faturaDoCiclo = cicloAtual?.let { c -> faturas.firstOrNull { it.competencia == c.competencia } }
        return when {
            cicloAtual == null -> estadoParaConfiguracaoIncompleta(hoje, faturas, lancamentos)
            faturaDoCiclo != null && hoje.isBefore(
                faturaDoCiclo.dataFechamento,
            ) -> configurarEstadoDeFatura(faturaDoCiclo, hoje, lancamentos)
            faturaDoCiclo != null -> estadoQuandoCicloFechou(instrumento, hoje, vigencias, faturas, lancamentos, faturaDoCiclo, cicloAtual)
            else -> estadoVazioDeCiclo(cicloAtual, hoje)
        }
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
        instrumento: Instrumento,
        hoje: LocalDate,
        vigencias: List<VigenciaDeCartao>,
        faturas: List<Fatura>,
        lancamentos: List<Lancamento>,
        faturaDoCiclo: Fatura,
        cicloAtual: CicloDeFatura,
    ): EstadoFaturaCartao {
        val proximaCompetencia = faturaDoCiclo.competencia.plusMonths(1)
        val faturaProxima = faturas.firstOrNull { it.competencia == proximaCompetencia }
        if (faturaProxima != null) {
            return configurarEstadoDeFatura(faturaProxima, hoje, lancamentos)
        }
        val vigencia = CalendarioDeFatura.resolverVigencia(instrumento.publicId, clock.instant(), vigencias)
        val proximoCiclo = CalendarioDeFatura.cicloDaCompetencia(
            competencia = proximaCompetencia,
            fechamentoDia = vigencia?.fechamentoDia ?: instrumento.fechamentoDia,
            vencimentoDia = vigencia?.vencimentoDia ?: instrumento.vencimentoDia,
        )
        val cicloAlvo = (proximoCiclo as? ResultadoCalculoCiclo.CicloCalculado)?.ciclo ?: cicloAtual
        return estadoVazioDeCiclo(cicloAlvo, hoje)
    }

    private fun resolverCicloAberto(
        instrumento: Instrumento,
        agora: java.time.Instant,
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

    private fun montarFaturasDoCartao(
        instrumento: Instrumento,
        conta: Conta?,
        vigencias: List<VigenciaDeCartao>,
        faturas: List<Fatura>,
        lancamentos: List<Lancamento>,
    ): FaturasDoCartao {
        val hoje = ZonedDateTime.ofInstant(clock.instant(), CalendarioDeFatura.ZONA_SAO_PAULO).toLocalDate()
        val estadoAberto = calcularEstadoFaturaAberta(instrumento, hoje, vigencias, faturas, lancamentos)
        val competenciaAberta = (estadoAberto as? EstadoFaturaCartao.Configurada)?.competencia
        val ciclos = faturas.sortedByDescending { it.competencia }.map { fatura ->
            montarCicloExistente(instrumento.publicId, fatura, hoje, competenciaAberta, lancamentos)
        }.toMutableList()
        if (estadoAberto is EstadoFaturaCartao.Configurada && faturas.none { it.competencia == estadoAberto.competencia }) {
            ciclos.add(0, montarCicloAbertoVazio(instrumento.publicId, estadoAberto))
        }
        return FaturasDoCartao(
            instrumento = instrumento,
            conta = conta,
            configuracaoIncompleta = estadoAberto is EstadoFaturaCartao.ConfiguracaoIncompleta,
            ciclos = ciclos.sortedByDescending { it.competencia },
        )
    }

    private fun montarCicloExistente(
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

    private fun montarCicloAbertoVazio(instrumentoPublicId: String, estadoAberto: EstadoFaturaCartao.Configurada): CicloDeFaturaDetalhado =
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

    private fun calcularUltimaRecarga(recargas: List<RecargaProgramada>, lancamentos: List<Lancamento>): UltimaRecargaVoucher? {
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

    private fun montarHistoricoAjustes(
        referencias: List<ReferenciaSaldoVoucher>,
        lancamentos: List<Lancamento>,
    ): List<HistoricoAjusteVoucher> {
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

    private data class CadastrosBase(
        val contas: List<Conta>,
        val instrumentos: List<Instrumento>,
        val vigencias: List<VigenciaDeCartao>,
        val faturas: List<Fatura>,
    )
}

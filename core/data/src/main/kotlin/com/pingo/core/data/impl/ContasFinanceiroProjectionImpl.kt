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
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.model.fatura.EstadoFaturaCartao
import com.pingo.core.model.fatura.FaturasDoCartao
import com.pingo.core.model.fatura.VigenciaDeCartao
import com.pingo.core.model.organizacao.ContaComIndicadores
import com.pingo.core.model.organizacao.IndicadorDeInstrumento
import com.pingo.core.model.organizacao.ResumoContasFinanceiro
import com.pingo.core.model.relatorio.AgregadorDeRelatorioMensal
import com.pingo.core.model.voucher.CalculadoraDeSaldoVoucher
import com.pingo.core.model.voucher.DetalheVoucher
import com.pingo.core.model.voucher.RecargaProgramada
import com.pingo.core.model.voucher.ReferenciaSaldoVoucher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import java.time.Clock
import java.time.YearMonth
import java.time.ZonedDateTime
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
            ultimaRecarga = CalculadorDeIndicadoresFinanceiros.calcularUltimaRecarga(recs, lancs),
            recargasProgramadas = recs,
            referencias = refs,
            historicoAjustes = CalculadorDeIndicadoresFinanceiros.montarHistoricoAjustes(refs, lancs),
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
                        agora = agora,
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
        agora: java.time.Instant,
        vigencias: List<VigenciaDeCartao>,
        faturas: List<Fatura>,
        referencias: List<ReferenciaSaldoVoucher>,
        recargas: List<RecargaProgramada>,
        lancamentos: List<Lancamento>,
    ): IndicadorDeInstrumento {
        val hoje = ZonedDateTime.ofInstant(agora, CalendarioDeFatura.ZONA_SAO_PAULO).toLocalDate()
        return when (instrumento.tipo) {
            TipoInstrumento.CREDITO -> IndicadorDeInstrumento.Credito(
                instrumento = instrumento,
                estadoFatura = CalculadorDeIndicadoresFinanceiros.calcularEstadoFaturaAberta(
                    instrumento = instrumento,
                    agora = agora,
                    hoje = hoje,
                    vigencias = vigencias,
                    faturas = faturas,
                    lancamentos = lancamentos,
                ),
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
                ultimaRecarga = CalculadorDeIndicadoresFinanceiros.calcularUltimaRecarga(recargas, lancamentos),
            )
        }
    }

    private fun montarFaturasDoCartao(
        instrumento: Instrumento,
        conta: Conta?,
        vigencias: List<VigenciaDeCartao>,
        faturas: List<Fatura>,
        lancamentos: List<Lancamento>,
    ): FaturasDoCartao {
        val agora = clock.instant()
        val hoje = ZonedDateTime.ofInstant(agora, CalendarioDeFatura.ZONA_SAO_PAULO).toLocalDate()
        val estadoAberto = CalculadorDeIndicadoresFinanceiros.calcularEstadoFaturaAberta(
            instrumento = instrumento,
            agora = agora,
            hoje = hoje,
            vigencias = vigencias,
            faturas = faturas,
            lancamentos = lancamentos,
        )
        val competenciaAberta = (estadoAberto as? EstadoFaturaCartao.Configurada)?.competencia
        val ciclos = faturas.sortedByDescending { it.competencia }.map { fatura ->
            CalculadorDeIndicadoresFinanceiros.montarCicloExistente(
                instrumentoPublicId = instrumento.publicId,
                fatura = fatura,
                hoje = hoje,
                competenciaAberta = competenciaAberta,
                lancamentos = lancamentos,
            )
        }.toMutableList()
        if (estadoAberto is EstadoFaturaCartao.Configurada && faturas.none { it.competencia == estadoAberto.competencia }) {
            ciclos.add(0, CalculadorDeIndicadoresFinanceiros.montarCicloAbertoVazio(instrumento.publicId, estadoAberto))
        }
        return FaturasDoCartao(
            instrumento = instrumento,
            conta = conta,
            configuracaoIncompleta = estadoAberto is EstadoFaturaCartao.ConfiguracaoIncompleta,
            ciclos = ciclos.sortedByDescending { it.competencia },
        )
    }

    private data class CadastrosBase(
        val contas: List<Conta>,
        val instrumentos: List<Instrumento>,
        val vigencias: List<VigenciaDeCartao>,
        val faturas: List<Fatura>,
    )
}

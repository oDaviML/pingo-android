package com.pingo.core.data.impl

import com.pingo.core.data.mapper.toModel
import com.pingo.core.data.mapper.toVigencia
import com.pingo.core.data.projecao.InicioFinanceiroProjection
import com.pingo.core.data.voucher.MaterializarRecargasVencidasUseCase
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Conta
import com.pingo.core.model.Fatura
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.model.fatura.EstadoFaturaCartao
import com.pingo.core.model.fatura.VigenciaDeCartao
import com.pingo.core.model.organizacao.EstadoBlocoFinanceiro
import com.pingo.core.model.organizacao.FaturaAbertaInicio
import com.pingo.core.model.organizacao.SaldoVoucherInicio
import com.pingo.core.model.voucher.CalculadoraDeSaldoVoucher
import com.pingo.core.model.voucher.RecargaProgramada
import com.pingo.core.model.voucher.ReferenciaSaldoVoucher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InicioFinanceiroProjectionImpl @Inject constructor(
    private val fontes: FontesContasProjection,
    private val materializarRecargasVencidas: MaterializarRecargasVencidasUseCase,
    private val clock: Clock,
) : InicioFinanceiroProjection {
    override fun observeFaturasAbertas(): Flow<EstadoBlocoFinanceiro<List<FaturaAbertaInicio>>> = combine(
        fontes.contaDao.observeAtivas(),
        fontes.instrumentoDao.observeAtivos(),
        fontes.configuracaoCartaoDao.observeAtivas(),
        fontes.faturaDao.observeAtivas(),
        fontes.lancamentoDao.observeAtivos(),
    ) { contas, ativos, vigencias, faturas, lancamentos ->
        val lista = montarFaturasAbertas(
            contas = contas.map { it.toModel() },
            ativos = ativos.map { it.toModel() },
            vigencias = vigencias.map { it.toVigencia() },
            faturas = faturas.map { it.toModel() },
            lancamentos = lancamentos.map { it.toModel() },
        )
        EstadoBlocoFinanceiro.Sucesso(lista) as EstadoBlocoFinanceiro<List<FaturaAbertaInicio>>
    }
        .onStart { emit(EstadoBlocoFinanceiro.Carregando) }
        .catch { emit(EstadoBlocoFinanceiro.Erro) }

    override fun observeSaldosVoucher(): Flow<EstadoBlocoFinanceiro<List<SaldoVoucherInicio>>> = combine(
        fontes.contaDao.observeAtivas(),
        fontes.instrumentoDao.observeAtivos(),
        fontes.referenciaDao.observeAtivas(),
        fontes.recargaDao.observeAtivas(),
        fontes.lancamentoDao.observeAtivos(),
    ) { contas, ativos, referencias, recargas, lancamentos ->
        val instrumentosAtivos = ativos.map { it.toModel() }
        val beneficioPadraoPorId = instrumentosAtivos.associate { it.publicId to (it.beneficio ?: BeneficioVoucher.VA) }
        val saldos = montarSaldosVoucher(
            contas = contas.map { it.toModel() },
            ativos = instrumentosAtivos,
            referencias = referencias.map { it.toModel() },
            recargas = recargas.map { rec -> rec.toModel(beneficioPadraoPorId[rec.instrumentoPublicId] ?: BeneficioVoucher.VA) },
            lancamentos = lancamentos.map { it.toModel() },
        )
        EstadoBlocoFinanceiro.Sucesso(saldos) as EstadoBlocoFinanceiro<List<SaldoVoucherInicio>>
    }
        .onStart {
            emit(EstadoBlocoFinanceiro.Carregando)
            runCatching { materializarRecargasVencidas() }
        }
        .catch { emit(EstadoBlocoFinanceiro.Erro) }

    private fun montarFaturasAbertas(
        contas: List<Conta>,
        ativos: List<Instrumento>,
        vigencias: List<VigenciaDeCartao>,
        faturas: List<Fatura>,
        lancamentos: List<Lancamento>,
    ): List<FaturaAbertaInicio> {
        val agora = clock.instant()
        val hoje = ZonedDateTime.ofInstant(agora, CalendarioDeFatura.ZONA_SAO_PAULO).toLocalDate()
        val contasPorId = contas.associateBy { it.publicId }
        val vigenciasPorInst = vigencias.groupBy { it.instrumentoPublicId }
        val faturasPorInst = faturas.groupBy { it.instrumentoPublicId }
        val lancsPorInst = lancamentos.filter { it.instrumentoPublicId != null }.groupBy { it.instrumentoPublicId!! }
        return ativos
            .asSequence()
            .filter { it.tipo == TipoInstrumento.CREDITO }
            .mapNotNull { cartao ->
                mapearFaturaAberta(
                    cartao = cartao,
                    conta = contasPorId[cartao.contaPublicId],
                    agora = agora,
                    hoje = hoje,
                    vigencias = vigenciasPorInst[cartao.publicId].orEmpty(),
                    faturas = faturasPorInst[cartao.publicId].orEmpty(),
                    lancamentos = lancsPorInst[cartao.publicId].orEmpty(),
                )
            }
            .sortedWith(compareBy<FaturaAbertaInicio> { it.dataVencimento }.thenBy { it.apelidoCartao })
            .toList()
    }

    private fun mapearFaturaAberta(
        cartao: Instrumento,
        conta: Conta?,
        agora: Instant,
        hoje: LocalDate,
        vigencias: List<VigenciaDeCartao>,
        faturas: List<Fatura>,
        lancamentos: List<Lancamento>,
    ): FaturaAbertaInicio? {
        val contaValida = conta ?: return null
        val estado = CalculadorDeIndicadoresFinanceiros.calcularEstadoFaturaAberta(
            instrumento = cartao,
            agora = agora,
            hoje = hoje,
            vigencias = vigencias,
            faturas = faturas,
            lancamentos = lancamentos,
        ) as? EstadoFaturaCartao.Configurada ?: return null
        return FaturaAbertaInicio(
            instrumentoPublicId = cartao.publicId,
            contaPublicId = contaValida.publicId,
            instituicao = contaValida.instituicao,
            apelidoCartao = cartao.apelido,
            ultimosDigitos = cartao.ultimosDigitos,
            faturaPublicId = estado.faturaPublicId,
            competencia = estado.competencia,
            dataFechamento = estado.dataFechamento,
            dataVencimento = estado.dataVencimento,
            valorCentavos = estado.totalCentavos,
            diasParaVencimento = estado.diasParaVencimento,
            status = estado.status,
        )
    }

    private fun montarSaldosVoucher(
        contas: List<Conta>,
        ativos: List<Instrumento>,
        referencias: List<ReferenciaSaldoVoucher>,
        recargas: List<RecargaProgramada>,
        lancamentos: List<Lancamento>,
    ): List<SaldoVoucherInicio> {
        val contasPorId = contas.associateBy { it.publicId }
        val refsPorInst = referencias.groupBy { it.instrumentoPublicId }
        val recsPorInst = recargas.groupBy { it.instrumentoPublicId }
        val lancsPorInst = lancamentos.filter { it.instrumentoPublicId != null }.groupBy { it.instrumentoPublicId!! }
        return ativos
            .asSequence()
            .filter { it.tipo == TipoInstrumento.VOUCHER }
            .mapNotNull { voucher ->
                mapearSaldoVoucher(
                    voucher = voucher,
                    conta = contasPorId[voucher.contaPublicId],
                    referencias = refsPorInst[voucher.publicId].orEmpty(),
                    recargas = recsPorInst[voucher.publicId].orEmpty(),
                    lancamentos = lancsPorInst[voucher.publicId].orEmpty(),
                )
            }
            .sortedWith(compareBy<SaldoVoucherInicio> { it.beneficio.ordinal }.thenBy { it.apelido })
            .toList()
    }

    private fun mapearSaldoVoucher(
        voucher: Instrumento,
        conta: Conta?,
        referencias: List<ReferenciaSaldoVoucher>,
        recargas: List<RecargaProgramada>,
        lancamentos: List<Lancamento>,
    ): SaldoVoucherInicio? {
        val contaValida = conta ?: return null
        val beneficio = voucher.beneficio ?: return null
        return SaldoVoucherInicio(
            instrumentoPublicId = voucher.publicId,
            contaPublicId = contaValida.publicId,
            instituicao = contaValida.instituicao,
            apelido = voucher.apelido,
            beneficio = beneficio,
            saldoCentavos = CalculadoraDeSaldoVoucher.calcularSaldoCentavos(voucher, referencias, lancamentos),
            ultimaRecarga = CalculadorDeIndicadoresFinanceiros.calcularUltimaRecarga(recargas, lancamentos),
        )
    }
}

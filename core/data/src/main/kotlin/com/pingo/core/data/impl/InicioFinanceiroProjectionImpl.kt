package com.pingo.core.data.impl

import com.pingo.core.data.mapper.toModel
import com.pingo.core.data.projecao.ContasFinanceiroProjection
import com.pingo.core.data.projecao.InicioFinanceiroProjection
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.model.fatura.EstadoFaturaCartao
import com.pingo.core.model.organizacao.EstadoBlocoFinanceiro
import com.pingo.core.model.organizacao.FaturaAbertaInicio
import com.pingo.core.model.organizacao.IndicadorDeInstrumento
import com.pingo.core.model.organizacao.SaldoVoucherInicio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InicioFinanceiroProjectionImpl @Inject constructor(
    private val contasProjection: ContasFinanceiroProjection,
    private val instrumentoDao: InstrumentoDao,
) : InicioFinanceiroProjection {
    override fun observeFaturasAbertas(): Flow<EstadoBlocoFinanceiro<List<FaturaAbertaInicio>>> =
        combine(contasProjection.observeContas(), instrumentoDao.observeAtivos()) { resumo, ativos ->
            val ativosIds = ativos.map { it.publicId }.toSet()
            val lista = resumo.contasAtivas
                .flatMap { contaCom ->
                    contaCom.instrumentos
                        .filterIsInstance<IndicadorDeInstrumento.Credito>()
                        .filter { it.instrumento.publicId in ativosIds }
                        .mapNotNull { indicador ->
                            val estado = indicador.estadoFatura as? EstadoFaturaCartao.Configurada ?: return@mapNotNull null
                            FaturaAbertaInicio(
                                instrumentoPublicId = indicador.instrumento.publicId,
                                contaPublicId = contaCom.conta.publicId,
                                instituicao = contaCom.conta.instituicao,
                                apelidoCartao = indicador.instrumento.apelido,
                                ultimosDigitos = indicador.instrumento.ultimosDigitos,
                                faturaPublicId = estado.faturaPublicId,
                                competencia = estado.competencia,
                                dataFechamento = estado.dataFechamento,
                                dataVencimento = estado.dataVencimento,
                                valorCentavos = estado.totalCentavos,
                                diasParaVencimento = estado.diasParaVencimento,
                                status = estado.status,
                            )
                        }
                }
                .sortedWith(compareBy<FaturaAbertaInicio> { it.dataVencimento }.thenBy { it.apelidoCartao })
            EstadoBlocoFinanceiro.Sucesso(lista) as EstadoBlocoFinanceiro<List<FaturaAbertaInicio>>
        }
            .onStart { emit(EstadoBlocoFinanceiro.Carregando) }
            .catch { emit(EstadoBlocoFinanceiro.Erro) }

    override fun observeSaldosVoucher(): Flow<EstadoBlocoFinanceiro<List<SaldoVoucherInicio>>> = contasProjection.observeContas()
        .map { resumo ->
            val saldos = resumo.contasAtivas
                .flatMap { contaCom ->
                    contaCom.instrumentos
                        .filterIsInstance<IndicadorDeInstrumento.Voucher>()
                        .mapNotNull { indicador ->
                            val beneficio = indicador.beneficio ?: return@mapNotNull null
                            SaldoVoucherInicio(
                                instrumentoPublicId = indicador.instrumento.publicId,
                                contaPublicId = contaCom.conta.publicId,
                                instituicao = contaCom.conta.instituicao,
                                apelido = indicador.instrumento.apelido,
                                beneficio = beneficio,
                                saldoCentavos = indicador.saldoCentavos,
                                ultimaRecarga = indicador.ultimaRecarga,
                            )
                        }
                }
                .sortedWith(compareBy<SaldoVoucherInicio> { it.beneficio.ordinal }.thenBy { it.apelido })
            EstadoBlocoFinanceiro.Sucesso(saldos) as EstadoBlocoFinanceiro<List<SaldoVoucherInicio>>
        }
        .onStart { emit(EstadoBlocoFinanceiro.Carregando) }
        .catch { emit(EstadoBlocoFinanceiro.Erro) }
}

package com.pingo.core.data.impl

import com.pingo.core.data.captura.ResolucaoDeInstrumento
import com.pingo.core.data.mapper.toModel
import com.pingo.core.data.reconciliacao.ReconciliadorVoucherCaptura
import com.pingo.core.data.reconciliacao.ResolucaoCapturaCaju
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.dao.RecargaProgramadaVoucherDao
import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.diaDoMesInformado
import com.pingo.core.model.voucher.BeneficioCajuMapper
import com.pingo.core.model.voucher.DepositoVoucherCapturado
import com.pingo.core.model.voucher.MotivoRevisaoRecarga
import com.pingo.core.model.voucher.OcorrenciaPrevista
import com.pingo.core.model.voucher.RecargaProgramada
import com.pingo.core.model.voucher.ReconciliadorDeRecarga
import com.pingo.core.model.voucher.ResultadoReconciliacaoRecarga
import com.pingo.core.model.voucher.StatusRecargaProgramada
import java.time.Instant
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReconciliadorVoucherCapturaImpl @Inject constructor(
    private val recargaDao: RecargaProgramadaVoucherDao,
    private val lancamentoDao: LancamentoDao,
) : ReconciliadorVoucherCaptura {
    override suspend fun resolverCaju(
        instrumentosAtivos: List<Instrumento>,
        beneficioBruto: String?,
        digitos: String?,
        ehDeposito: Boolean,
        valorCentavos: Long,
        dataHora: Instant,
    ): ResolucaoCapturaCaju {
        val beneficio = BeneficioCajuMapper.mapear(beneficioBruto)
        val vouchers = instrumentosAtivos.filter { it.tipo == TipoInstrumento.VOUCHER }
        if (!ehDeposito) {
            return resolverSaidaCaju(beneficio, digitos, vouchers, instrumentosAtivos)
        }
        return resolverDepositoCaju(beneficioBruto, beneficio, digitos, vouchers, valorCentavos, dataHora)
    }

    override suspend fun confirmarEfeitoAposGravar(lancamento: Lancamento, resolucao: ResolucaoCapturaCaju, agora: Instant) {
        resolucao.lancamentoPrevisaoParaExcluirPublicId?.let { previsaoLancamentoId ->
            lancamentoDao.marcarExcluido(previsaoLancamentoId, agora)
        }
        val substituirId = resolucao.ocorrenciaParaSubstituirPublicId
        val paraSubstituir = substituirId?.let { recargaDao.buscar(it) }
        val novaPendente = resolucao.ocorrenciaParaCriarPendente
        when {
            paraSubstituir != null -> {
                recargaDao.upsert(
                    paraSubstituir.copy(
                        status = StatusRecargaProgramada.SUBSTITUIDA,
                        lancamentoPublicId = lancamento.publicId,
                        atualizadoEm = agora,
                    ),
                )
            }
            substituirId != null && lancamento.instrumentoPublicId != null -> {
                val instrumentoId = lancamento.instrumentoPublicId
                val dataLocal = ZonedDateTime.ofInstant(lancamento.dataHora, ReconciliadorDeRecarga.ZONA_SAO_PAULO).toLocalDate()
                recargaDao.upsert(
                    RecargaProgramadaVoucherEntity(
                        publicId = substituirId,
                        instrumentoPublicId = instrumentoId!!,
                        beneficio = lancamento.beneficio,
                        competenciaMes = dataLocal.withDayOfMonth(1),
                        dataPrevista = dataLocal,
                        valorPrevistoCentavos = lancamento.valorCentavos,
                        status = StatusRecargaProgramada.SUBSTITUIDA,
                        lancamentoPublicId = lancamento.publicId,
                        criadoEm = agora,
                        atualizadoEm = agora,
                        excluidoEm = null,
                    ),
                )
            }
            novaPendente != null -> garantirPrevisaoPendente(novaPendente, agora)
        }
    }

    private fun resolverSaidaCaju(
        beneficio: BeneficioVoucher?,
        digitos: String?,
        vouchers: List<Instrumento>,
        todos: List<Instrumento>,
    ): ResolucaoCapturaCaju {
        val pool = when {
            beneficio != null -> vouchers.filter { it.beneficio == beneficio }
            vouchers.isNotEmpty() -> vouchers
            else -> todos
        }
        val resolucao = ResolucaoDeInstrumento.de(digitos, pool)
        return ResolucaoCapturaCaju(
            beneficio = beneficio,
            instrumento = resolucao.instrumento,
            motivo = resolucao.motivo,
        )
    }

    private suspend fun resolverDepositoCaju(
        beneficioBruto: String?,
        beneficio: BeneficioVoucher?,
        digitos: String?,
        vouchers: List<Instrumento>,
        valorCentavos: Long,
        dataHora: Instant,
    ): ResolucaoCapturaCaju {
        val alvo = when {
            beneficio != null -> resolverComBeneficioExplicitado(beneficio, digitos, vouchers)
            !beneficioBruto.isNullOrBlank() -> AlvoDepositoCaju.Pendente(MotivoPendencia.BENEFICIO_AMBIGUO)
            vouchers.isEmpty() -> AlvoDepositoCaju.Pendente(MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO)
            vouchers.size == 1 && vouchers.single().beneficio != null ->
                AlvoDepositoCaju.Resolvido(vouchers.single(), requireNotNull(vouchers.single().beneficio))
            else -> AlvoDepositoCaju.Pendente(MotivoPendencia.BENEFICIO_AMBIGUO)
        }
        return when (alvo) {
            is AlvoDepositoCaju.Pendente -> ResolucaoCapturaCaju(beneficio = beneficio, instrumento = null, motivo = alvo.motivo)
            is AlvoDepositoCaju.Resolvido -> conciliarDepositoNoInstrumento(alvo.instrumento, alvo.beneficio, valorCentavos, dataHora)
        }
    }

    private fun resolverComBeneficioExplicitado(
        beneficio: BeneficioVoucher,
        digitos: String?,
        vouchers: List<Instrumento>,
    ): AlvoDepositoCaju {
        val candidatos = vouchers.filter { it.beneficio == beneficio }
        val resolucao = ResolucaoDeInstrumento.de(digitos, candidatos)
        val inst = resolucao.instrumento
        return if (inst != null) {
            AlvoDepositoCaju.Resolvido(inst, beneficio)
        } else {
            AlvoDepositoCaju.Pendente(resolucao.motivo ?: MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO)
        }
    }

    private sealed interface AlvoDepositoCaju {
        data class Resolvido(val instrumento: Instrumento, val beneficio: BeneficioVoucher) : AlvoDepositoCaju

        data class Pendente(val motivo: MotivoPendencia) : AlvoDepositoCaju
    }

    private suspend fun conciliarDepositoNoInstrumento(
        instrumento: Instrumento,
        beneficio: BeneficioVoucher,
        valorCentavos: Long,
        dataHora: Instant,
    ): ResolucaoCapturaCaju {
        val existentesBanco = recargaDao.listarPorInstrumento(instrumento.publicId)
        val mapaEfetivadasSubstituiveis = linkedMapOf<String, String>()
        val ocorrencias = montarOcorrenciasCandidatas(existentesBanco, beneficio, mapaEfetivadasSubstituiveis)
        val competenciaMes = ZonedDateTime.ofInstant(dataHora, ReconciliadorDeRecarga.ZONA_SAO_PAULO).toLocalDate().withDayOfMonth(1)
        val sintetica = sintetizarPrevisaoDoMesSeAusente(instrumento, beneficio, competenciaMes, existentesBanco)
        adicionarPrevisaoSintetica(ocorrencias, instrumento, beneficio, sintetica)
        return interpretarResultadoConciliacao(
            conciliacao = ReconciliadorDeRecarga.conciliarDeposito(
                DepositoVoucherCapturado(instrumento.publicId, beneficio, valorCentavos, dataHora),
                ocorrencias,
            ),
            beneficio = beneficio,
            instrumento = instrumento,
            mapaEfetivadasSubstituiveis = mapaEfetivadasSubstituiveis,
            sintetica = sintetica,
        )
    }

    private suspend fun montarOcorrenciasCandidatas(
        existentesBanco: List<RecargaProgramadaVoucherEntity>,
        beneficio: BeneficioVoucher,
        mapaEfetivadasSubstituiveis: MutableMap<String, String>,
    ): MutableList<RecargaProgramada> {
        val ocorrencias = mutableListOf<RecargaProgramada>()
        for (entity in existentesBanco) {
            when (entity.status) {
                StatusRecargaProgramada.PENDENTE -> ocorrencias.add(entity.toModel(beneficio))
                StatusRecargaProgramada.EFETIVADA -> {
                    val lancPrev = entity.lancamentoPublicId?.let { lancamentoDao.buscar(it) }
                    if (lancPrev != null && lancPrev.excluidoEm == null && lancPrev.origem == OrigemLancamento.MANUAL) {
                        mapaEfetivadasSubstituiveis[entity.publicId] = lancPrev.publicId
                        ocorrencias.add(entity.toModel(beneficio).copy(status = StatusRecargaProgramada.PENDENTE))
                    }
                }
                StatusRecargaProgramada.SUBSTITUIDA, StatusRecargaProgramada.IGNORADA -> Unit
            }
        }
        return ocorrencias
    }

    private fun adicionarPrevisaoSintetica(
        ocorrencias: MutableList<RecargaProgramada>,
        instrumento: Instrumento,
        beneficio: BeneficioVoucher,
        sintetica: OcorrenciaPrevista?,
    ) {
        if (sintetica == null) {
            return
        }
        ocorrencias.add(
            RecargaProgramada(
                publicId = UUID.randomUUID().toString(),
                instrumentoPublicId = instrumento.publicId,
                beneficio = beneficio,
                competenciaMes = sintetica.competenciaMes,
                dataPrevista = sintetica.dataPrevista,
                valorPrevistoCentavos = sintetica.valorPrevistoCentavos,
                status = StatusRecargaProgramada.PENDENTE,
            ),
        )
    }

    private fun interpretarResultadoConciliacao(
        conciliacao: ResultadoReconciliacaoRecarga,
        beneficio: BeneficioVoucher,
        instrumento: Instrumento,
        mapaEfetivadasSubstituiveis: Map<String, String>,
        sintetica: OcorrenciaPrevista?,
    ): ResolucaoCapturaCaju = when (conciliacao) {
        is ResultadoReconciliacaoRecarga.SubstituicaoUnica -> ResolucaoCapturaCaju(
            beneficio = beneficio,
            instrumento = instrumento,
            motivo = null,
            ocorrenciaParaSubstituirPublicId = conciliacao.ocorrencia.publicId,
            lancamentoPrevisaoParaExcluirPublicId = mapaEfetivadasSubstituiveis[conciliacao.ocorrencia.publicId],
        )
        is ResultadoReconciliacaoRecarga.RequerRevisao -> ResolucaoCapturaCaju(
            beneficio = beneficio,
            instrumento = instrumento,
            motivo = when (conciliacao.motivo) {
                MotivoRevisaoRecarga.BENEFICIO_AMBIGUO -> MotivoPendencia.BENEFICIO_AMBIGUO
                MotivoRevisaoRecarga.DIVERGENCIA_RECARGA -> MotivoPendencia.DIVERGENCIA_RECARGA
            },
            ocorrenciaParaCriarPendente = sintetica,
        )
        ResultadoReconciliacaoRecarga.SemPrevisaoCompativel -> ResolucaoCapturaCaju(
            beneficio = beneficio,
            instrumento = instrumento,
            motivo = null,
        )
    }

    private fun sintetizarPrevisaoDoMesSeAusente(
        instrumento: Instrumento,
        beneficio: BeneficioVoucher,
        competenciaMes: java.time.LocalDate,
        existentes: List<RecargaProgramadaVoucherEntity>,
    ): OcorrenciaPrevista? {
        val dia = instrumento.diaRecarga
        val valor = instrumento.valorRecargaCentavos
        val podeGerar = diaDoMesInformado(dia) && (valor ?: 0L) > 0L && existentes.none { it.competenciaMes == competenciaMes }
        if (!podeGerar || dia == null || valor == null) {
            return null
        }
        val dataPrevista = ReconciliadorDeRecarga.dataPrevistaNoMes(YearMonth.from(competenciaMes), dia)
        val criadoLocal = ZonedDateTime.ofInstant(instrumento.criadoEm, ReconciliadorDeRecarga.ZONA_SAO_PAULO).toLocalDate()
        return OcorrenciaPrevista(
            instrumentoPublicId = instrumento.publicId,
            beneficio = beneficio,
            competenciaMes = competenciaMes,
            dataPrevista = dataPrevista,
            valorPrevistoCentavos = valor,
        ).takeUnless { dataPrevista.isBefore(criadoLocal) }
    }

    private suspend fun garantirPrevisaoPendente(previsao: OcorrenciaPrevista, agora: Instant) {
        if (recargaDao.buscarPorCompetencia(previsao.instrumentoPublicId, previsao.competenciaMes) != null) {
            return
        }
        recargaDao.upsert(
            RecargaProgramadaVoucherEntity(
                publicId = UUID.randomUUID().toString(),
                instrumentoPublicId = previsao.instrumentoPublicId,
                beneficio = previsao.beneficio,
                competenciaMes = previsao.competenciaMes,
                dataPrevista = previsao.dataPrevista,
                valorPrevistoCentavos = previsao.valorPrevistoCentavos,
                status = StatusRecargaProgramada.PENDENTE,
                lancamentoPublicId = null,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
    }
}

package com.pingo.core.model.voucher

import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Instrumento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.diaDoMesInformado
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

object ReconciliadorDeRecarga {
    val ZONA_SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")

    fun dataPrevistaNoMes(mes: YearMonth, diaRecarga: Int): LocalDate = mes.atDay(minOf(diaRecarga, mes.lengthOfMonth()))

    fun gerarOcorrenciasDevidas(
        instrumento: Instrumento,
        desdeDataInclusiva: LocalDate,
        hoje: LocalDate,
        existentes: List<RecargaProgramada>,
    ): List<OcorrenciaPrevista> {
        if (!instrumentoElegivelParaRecarga(instrumento) || hoje.isBefore(desdeDataInclusiva)) {
            return emptyList()
        }
        val competenciasExistentes = existentes
            .asSequence()
            .filter { it.instrumentoPublicId == instrumento.publicId && it.excluidoEm == null }
            .map { it.competenciaMes.withDayOfMonth(1) }
            .toSet()

        return gerarNoIntervalo(
            instrumento = instrumento,
            desdeDataInclusiva = desdeDataInclusiva,
            hoje = hoje,
            competenciasExistentes = competenciasExistentes,
        )
    }

    fun conciliarDeposito(
        deposito: DepositoVoucherCapturado,
        ocorrenciasExistentes: List<RecargaProgramada>,
    ): ResultadoReconciliacaoRecarga {
        val pendentes = ocorrenciasExistentes.filter {
            it.excluidoEm == null && it.status == StatusRecargaProgramada.PENDENTE
        }
        if (deposito.beneficio == null || deposito.instrumentoPublicId == null) {
            return ResultadoReconciliacaoRecarga.RequerRevisao(
                motivo = MotivoRevisaoRecarga.BENEFICIO_AMBIGUO,
                candidatos = candidatosParaBeneficioAmbiguo(deposito.instrumentoPublicId, pendentes),
            )
        }
        return conciliarComBeneficioDefinido(
            instrumentoPublicId = deposito.instrumentoPublicId,
            beneficio = deposito.beneficio,
            valorCentavos = deposito.valorCentavos,
            dataLocal = ZonedDateTime.ofInstant(deposito.dataHora, ZONA_SAO_PAULO).toLocalDate(),
            pendentes = pendentes,
        )
    }

    private fun instrumentoElegivelParaRecarga(instrumento: Instrumento): Boolean = instrumento.tipo == TipoInstrumento.VOUCHER &&
        !instrumento.arquivado &&
        instrumento.excluidoEm == null &&
        instrumento.beneficio != null &&
        diaDoMesInformado(instrumento.diaRecarga) &&
        (instrumento.valorRecargaCentavos ?: 0L) > 0L

    private fun gerarNoIntervalo(
        instrumento: Instrumento,
        desdeDataInclusiva: LocalDate,
        hoje: LocalDate,
        competenciasExistentes: Set<LocalDate>,
    ): List<OcorrenciaPrevista> {
        val dia = requireNotNull(instrumento.diaRecarga)
        val beneficio = requireNotNull(instrumento.beneficio)
        val valor = requireNotNull(instrumento.valorRecargaCentavos)
        val resultado = mutableListOf<OcorrenciaPrevista>()
        var mesAtual = YearMonth.from(desdeDataInclusiva)
        val mesFinal = YearMonth.from(hoje)

        while (!mesAtual.isAfter(mesFinal)) {
            val competencia = mesAtual.atDay(1)
            val dataPrevista = dataPrevistaNoMes(mesAtual, dia)
            if (!dataPrevista.isBefore(desdeDataInclusiva) && !dataPrevista.isAfter(hoje) && competencia !in competenciasExistentes) {
                resultado.add(
                    OcorrenciaPrevista(
                        instrumentoPublicId = instrumento.publicId,
                        beneficio = beneficio,
                        competenciaMes = competencia,
                        dataPrevista = dataPrevista,
                        valorPrevistoCentavos = valor,
                    ),
                )
            }
            mesAtual = mesAtual.plusMonths(1)
        }
        return resultado
    }

    private fun candidatosParaBeneficioAmbiguo(instrumentoPublicId: String?, pendentes: List<RecargaProgramada>): List<RecargaProgramada> =
        if (instrumentoPublicId != null) {
            pendentes.filter { it.instrumentoPublicId == instrumentoPublicId }
        } else {
            pendentes
        }

    private fun conciliarComBeneficioDefinido(
        instrumentoPublicId: String,
        beneficio: BeneficioVoucher,
        valorCentavos: Long,
        dataLocal: LocalDate,
        pendentes: List<RecargaProgramada>,
    ): ResultadoReconciliacaoRecarga {
        val correspondenciasExatas = pendentes.filter {
            it.instrumentoPublicId == instrumentoPublicId &&
                it.beneficio == beneficio &&
                it.valorPrevistoCentavos == valorCentavos &&
                it.dataPrevista == dataLocal
        }
        val pendentesDoBeneficio = pendentes.filter {
            it.instrumentoPublicId == instrumentoPublicId && it.beneficio == beneficio
        }
        return when {
            correspondenciasExatas.size == 1 ->
                ResultadoReconciliacaoRecarga.SubstituicaoUnica(correspondenciasExatas.single())
            correspondenciasExatas.size > 1 ->
                ResultadoReconciliacaoRecarga.RequerRevisao(MotivoRevisaoRecarga.DIVERGENCIA_RECARGA, correspondenciasExatas)
            pendentesDoBeneficio.isNotEmpty() ->
                ResultadoReconciliacaoRecarga.RequerRevisao(MotivoRevisaoRecarga.DIVERGENCIA_RECARGA, pendentesDoBeneficio)
            else -> ResultadoReconciliacaoRecarga.SemPrevisaoCompativel
        }
    }
}

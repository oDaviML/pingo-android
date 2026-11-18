package com.pingo.core.model.assinatura

import com.pingo.core.model.Assinatura
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object DetectorDeAssinaturas {
    val ZONA_SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")
    const val INTERVALO_MINIMO_DIAS: Long = 28L
    const val INTERVALO_MAXIMO_DIAS: Long = 35L
    const val PERCENTUAL_MINIMO_VALOR: Long = 90L
    const val PERCENTUAL_MAXIMO_VALOR: Long = 110L

    fun elegivelParaDeteccao(lancamento: Lancamento): Boolean = lancamento.excluidoEm == null &&
        lancamento.estornoDePublicId == null &&
        lancamento.status == StatusLancamento.CONFIRMADO &&
        lancamento.tipo == TipoLancamento.DESPESA &&
        lancamento.instrumentoPublicId != null &&
        lancamento.valorCentavos > 0L &&
        NormalizadorDeAssinatura.normalizar(lancamento.estabelecimento) != null

    fun intervaloValido(dataAnterior: LocalDate, dataAtual: LocalDate): Boolean =
        ChronoUnit.DAYS.between(dataAnterior, dataAtual) in INTERVALO_MINIMO_DIAS..INTERVALO_MAXIMO_DIAS

    fun valorNaFaixa(valorAnteriorCentavos: Long, valorAtualCentavos: Long): Boolean = valorAnteriorCentavos > 0L &&
        valorAtualCentavos > 0L &&
        (valorAtualCentavos * 100L) in (valorAnteriorCentavos * PERCENTUAL_MINIMO_VALOR)..(valorAnteriorCentavos * PERCENTUAL_MAXIMO_VALOR)

    fun calcularProximaCobranca(ultimaCobranca: LocalDate): LocalDate {
        val proximoMes = YearMonth.from(ultimaCobranca).plusMonths(1)
        return proximoMes.atDay(minOf(ultimaCobranca.dayOfMonth, proximoMes.lengthOfMonth()))
    }

    fun detectarSugestoes(lancamentos: List<Lancamento>, chavesIgnoradas: Set<ChaveAssinatura> = emptySet()): List<SugestaoDeAssinatura> {
        val agrupados = lancamentos
            .asSequence()
            .filter { elegivelParaDeteccao(it) }
            .groupBy { lancamento ->
                ChaveAssinatura(
                    instrumentoPublicId = requireNotNull(lancamento.instrumentoPublicId),
                    estabelecimentoNormalizado = requireNotNull(NormalizadorDeAssinatura.normalizar(lancamento.estabelecimento)),
                )
            }

        return agrupados
            .asSequence()
            .filter { (chave, _) -> chave !in chavesIgnoradas }
            .mapNotNull { (chave, itens) -> construirSugestao(chave, itens) }
            .sortedWith(
                compareBy<SugestaoDeAssinatura> { it.chave.estabelecimentoNormalizado }
                    .thenBy { it.chave.instrumentoPublicId },
            )
            .toList()
    }

    fun totalMensalConfirmadasCentavos(assinaturas: List<Assinatura>): Long = assinaturas
        .asSequence()
        .filter { it.excluidoEm == null && it.status == StatusAssinatura.CONFIRMADA }
        .sumOf { it.valorReferenciaCentavos }

    fun correspondeACobrancaConfirmada(lancamento: Lancamento, assinatura: Assinatura, instrumentoDaAssinatura: String?): Boolean {
        if (!elegivelParaDeteccao(lancamento) || assinatura.excluidoEm != null || assinatura.status != StatusAssinatura.CONFIRMADA) {
            return false
        }
        if (instrumentoDaAssinatura == null || lancamento.instrumentoPublicId != instrumentoDaAssinatura) {
            return false
        }
        val normalizado = NormalizadorDeAssinatura.normalizar(lancamento.estabelecimento)
        val dataLocal = ZonedDateTime.ofInstant(lancamento.dataHora, ZONA_SAO_PAULO).toLocalDate()
        val ultima = assinatura.ultimaCobranca
        val intervaloOk = ultima == null || intervaloValido(ultima, dataLocal)
        val valorOk = assinatura.valorReferenciaCentavos <= 0L || valorNaFaixa(assinatura.valorReferenciaCentavos, lancamento.valorCentavos)
        return normalizado == assinatura.estabelecimentoNormalizado && intervaloOk && valorOk
    }

    private fun construirSugestao(chave: ChaveAssinatura, itens: List<Lancamento>): SugestaoDeAssinatura? {
        if (itens.size < 2) {
            return null
        }
        val ordenados = itens.sortedWith(compareBy<Lancamento> { it.dataHora }.thenBy { it.publicId })
        val sequencia = ultimaSequenciaQualificadora(ordenados)
        if (sequencia.size < 2) {
            return null
        }
        val ultima = sequencia.last()
        val ultimaData = ZonedDateTime.ofInstant(ultima.dataHora, ZONA_SAO_PAULO).toLocalDate()
        return SugestaoDeAssinatura(
            chave = chave,
            estabelecimentoExibicao = requireNotNull(ultima.estabelecimento).trim(),
            valorReferenciaCentavos = ultima.valorCentavos,
            ultimaCobranca = ultimaData,
            proximaCobrancaEstimada = calcularProximaCobranca(ultimaData),
            lancamentosQualificadoresPublicIds = sequencia.map { it.publicId },
        )
    }

    private fun ultimaSequenciaQualificadora(ordenados: List<Lancamento>): List<Lancamento> {
        var melhorSequencia: List<Lancamento> = emptyList()
        var sequenciaAtual = mutableListOf(ordenados.first())
        for (i in 1 until ordenados.size) {
            val anterior = ordenados[i - 1]
            val atual = ordenados[i]
            if (parQualificador(anterior, atual)) {
                sequenciaAtual.add(atual)
            } else {
                if (sequenciaAtual.size >= 2) {
                    melhorSequencia = sequenciaAtual.toList()
                }
                sequenciaAtual = mutableListOf(atual)
            }
        }
        if (sequenciaAtual.size >= 2) {
            melhorSequencia = sequenciaAtual.toList()
        }
        return melhorSequencia
    }

    private fun parQualificador(anterior: Lancamento, atual: Lancamento): Boolean {
        val dataAnterior = ZonedDateTime.ofInstant(anterior.dataHora, ZONA_SAO_PAULO).toLocalDate()
        val dataAtual = ZonedDateTime.ofInstant(atual.dataHora, ZONA_SAO_PAULO).toLocalDate()
        return intervaloValido(dataAnterior, dataAtual) && valorNaFaixa(anterior.valorCentavos, atual.valorCentavos)
    }
}

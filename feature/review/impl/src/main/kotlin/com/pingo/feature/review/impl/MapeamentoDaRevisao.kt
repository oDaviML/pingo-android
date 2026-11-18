package com.pingo.feature.review.impl

import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.voucher.CandidatosRevisaoVoucher
import com.pingo.core.model.voucher.MotivoRevisaoRecarga
import com.pingo.core.ui.Valores
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

internal data class BaseDaRevisao(
    val carregado: Boolean = false,
    val lancamentos: List<Lancamento> = emptyList(),
    val contas: List<Conta> = emptyList(),
    val instrumentos: List<Instrumento> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
)

internal data class ControleDaRevisao(
    val painelDeCategoria: TrocaDeCategoriaDaRevisao? = null,
    val painelDeTransferencia: MarcarTransferenciaDaRevisao? = null,
    val painelDeVoucher: ResolucaoDeVoucherDaRevisao? = null,
    val ocupado: Boolean = false,
    val mensagem: MensagemDaRevisao? = null,
)

internal fun montarRevisao(base: BaseDaRevisao, controle: ControleDaRevisao, agora: Instant, zona: ZoneId): ReviewUiState {
    val contasPorId = base.contas.associateBy { it.publicId }
    val instrumentosPorId = base.instrumentos.associateBy { it.publicId }
    val categoriasPorId = base.categorias.associateBy { it.publicId }
    val pendentes = base.lancamentos
        .filter { it.status == StatusLancamento.PENDENTE && it.excluidoEm == null }
        .sortedByDescending { it.dataHora }
        .map { lancamento ->
            val instrumento = instrumentoValido(lancamento, instrumentosPorId)
            val conta = instrumento?.let { contasPorId[it.contaPublicId] }
            val motivo = motivoDe(lancamento.motivoPendencia)
            val ehRevisaoVoucher = motivo == MotivoDaRevisao.BENEFICIO_AMBIGUO ||
                motivo == MotivoDaRevisao.DIVERGENCIA_RECARGA

            PendenciaDaRevisao(
                publicId = lancamento.publicId,
                motivo = motivo,
                confianca = lancamento.confianca,
                estabelecimento = lancamento.estabelecimento,
                valorCentavos = lancamento.valorCentavos,
                tipo = lancamento.tipo,
                dataHora = lancamento.dataHora,
                horario = horarioDe(lancamento.dataHora, agora, zona),
                vinculo = rotuloDoVinculo(instrumento, conta),
                categoriaNome = lancamento.categoriaPublicId?.let { categoriasPorId[it]?.nome },
                textoInterpretado = trechoDe(lancamento.textoBruto),
                podeConfirmar = instrumento != null && !ehRevisaoVoucher,
            )
        }

    return ReviewUiState(
        carregando = !base.carregado,
        pendentes = pendentes,
        painelDeCategoria = controle.painelDeCategoria,
        painelDeTransferencia = controle.painelDeTransferencia,
        painelDeVoucher = controle.painelDeVoucher,
        ocupado = controle.ocupado,
        mensagem = controle.mensagem,
    )
}

internal fun instrumentoValido(lancamento: Lancamento, instrumentosPorId: Map<String, Instrumento>): Instrumento? = lancamento
    .instrumentoPublicId
    ?.let { instrumentosPorId[it] }
    ?.takeIf { it.excluidoEm == null }

internal fun rotuloDoVinculo(instrumento: Instrumento?, conta: Conta?): String {
    if (instrumento == null) {
        return "Instrumento não identificado"
    }

    val banco = conta?.apelido ?: instrumento.apelido

    return "$banco · ${tipoDoInstrumento(instrumento)}"
}

internal fun rotuloDoInstrumento(contasPorId: Map<String, Conta>, instrumento: Instrumento): String {
    val banco = contasPorId[instrumento.contaPublicId]?.apelido ?: instrumento.apelido

    return "$banco · ${tipoDoInstrumento(instrumento)}"
}

private fun tipoDoInstrumento(instrumento: Instrumento): String = when (instrumento.tipo) {
    TipoInstrumento.CREDITO -> "crédito"
    TipoInstrumento.DEBITO -> "débito"
    TipoInstrumento.VOUCHER -> instrumento.beneficio?.name ?: "voucher"
}

internal fun categoriasDoTipo(categorias: List<Categoria>, tipo: TipoLancamento): List<OpcaoDaRevisao> {
    val desejado = when (tipo) {
        TipoLancamento.DESPESA -> TipoCategoria.DESPESA
        TipoLancamento.RECEITA -> TipoCategoria.RECEITA
        TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> return emptyList()
    }

    return categorias
        .filter { it.podeSerEscolhida && it.tipo == desejado }
        .sortedBy { it.nome }
        .map { OpcaoDaRevisao(it.publicId, it.nome) }
}

internal fun instrumentosParaTransferencia(base: BaseDaRevisao, lancamento: Lancamento): List<OpcaoDaRevisao> {
    val contaOrigem = lancamento.instrumentoPublicId
        ?.let { publicId -> base.instrumentos.firstOrNull { it.publicId == publicId } }
        ?.contaPublicId
    val contasPorId = base.contas.associateBy { it.publicId }

    return base.instrumentos
        .filter { it.excluidoEm == null && !it.arquivado && it.contaPublicId != contaOrigem }
        .filter { instrumento -> contasPorId[instrumento.contaPublicId]?.let { it.excluidoEm == null && !it.arquivada } == true }
        .map { OpcaoDaRevisao(it.publicId, rotuloDoInstrumento(contasPorId, it)) }
}

internal fun trechoDe(texto: String?): String? {
    val limpo = texto?.trim()?.takeIf { it.isNotEmpty() } ?: return null

    return if (limpo.length <= LIMITE_DO_TRECHO) limpo else limpo.take(LIMITE_DO_TRECHO).trimEnd() + "…"
}

internal fun horarioDe(dataHora: Instant, agora: Instant, zona: ZoneId): String {
    val momento = dataHora.atZone(zona)
    val hoje = agora.atZone(zona).toLocalDate()
    val dia = when (momento.toLocalDate()) {
        hoje -> "hoje"
        hoje.minusDays(1) -> "ontem"
        else -> DATA.format(momento)
    }

    return "$dia, ${HORA.format(momento)}"
}

internal fun ehPendenciaVoucher(motivo: String?): Boolean = motivo == MotivoPendencia.BENEFICIO_AMBIGUO.name ||
    motivo == MotivoPendencia.DIVERGENCIA_RECARGA.name

internal fun montarPainelDeVoucher(candidatos: CandidatosRevisaoVoucher, contas: List<Conta>, zona: ZoneId): ResolucaoDeVoucherDaRevisao {
    val contasPorId = contas.associateBy { it.publicId }
    val vouchers = candidatos.vouchersElegiveis.map { voucherElegivelDe(it, contasPorId) }
    val ocorrencias = candidatos.ocorrenciasCandidatas.map { recarga ->
        OcorrenciaPrevistaDaRevisao(
            publicId = recarga.publicId,
            instrumentoPublicId = recarga.instrumentoPublicId,
            beneficio = recarga.beneficio,
            dataPrevista = recarga.dataPrevista,
            dataPrevistaFormatada = DATA_COMPLETA.format(recarga.dataPrevista),
            valorPrevistoCentavos = recarga.valorPrevistoCentavos,
        )
    }
    val voucherInicial = if (candidatos.motivo == MotivoRevisaoRecarga.DIVERGENCIA_RECARGA) {
        candidatos.lancamento.instrumentoPublicId?.takeIf { id -> vouchers.any { it.publicId == id } }
            ?: vouchers.firstOrNull()?.publicId
    } else {
        null
    }
    val dataLancamento = candidatos.lancamento.dataHora.atZone(zona).toLocalDate()
    val ocorrenciaInicial = voucherInicial?.let { id ->
        ocorrenciaMaisProxima(ocorrencias.filter { it.instrumentoPublicId == id }, dataLancamento)
    }
    return ResolucaoDeVoucherDaRevisao(
        lancamentoPublicId = candidatos.lancamento.publicId,
        motivo = candidatos.motivo,
        estabelecimento = candidatos.lancamento.estabelecimento ?: "Recarga de voucher",
        valorCapturadoCentavos = candidatos.lancamento.valorCentavos,
        vouchersElegiveis = vouchers,
        ocorrenciasCandidatas = ocorrencias,
        voucherSelecionadoPublicId = voucherInicial,
        ocorrenciaSelecionadaPublicId = ocorrenciaInicial,
    )
}

private fun voucherElegivelDe(instrumento: Instrumento, contasPorId: Map<String, Conta>): VoucherElegivelDaRevisao {
    val conta = contasPorId[instrumento.contaPublicId]
    val tipo = instrumento.beneficio?.name ?: "Voucher"
    val contaNome = conta?.apelido ?: instrumento.apelido
    val partesDetalhe = buildList {
        instrumento.apelido.takeIf { conta != null && it.isNotBlank() && it != conta.apelido }?.let(::add)
        instrumento.ultimosDigitos?.takeIf { it.isNotBlank() }?.let { add("final $it") }
        instrumento.saldoCentavos?.let { add("Saldo ${Valores.formatar(it)}") }
    }
    return VoucherElegivelDaRevisao(
        publicId = instrumento.publicId,
        beneficio = instrumento.beneficio,
        titulo = "$tipo · $contaNome",
        detalhe = partesDetalhe.joinToString(" · ").takeIf { it.isNotEmpty() },
    )
}

internal fun ocorrenciaMaisProxima(ocorrencias: List<OcorrenciaPrevistaDaRevisao>, dataReferencia: LocalDate): String? = ocorrencias
    .minWithOrNull(
        compareBy<OcorrenciaPrevistaDaRevisao> { abs(ChronoUnit.DAYS.between(it.dataPrevista, dataReferencia)) }
            .thenByDescending { it.dataPrevista },
    )
    ?.publicId

private const val LIMITE_DO_TRECHO = 160
private val DATA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR"))
private val DATA_COMPLETA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
private val HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR"))

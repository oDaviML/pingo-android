package com.pingo.feature.review.impl

import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

            PendenciaDaRevisao(
                publicId = lancamento.publicId,
                motivo = motivoDe(lancamento.motivoPendencia),
                confianca = lancamento.confianca,
                estabelecimento = lancamento.estabelecimento,
                valorCentavos = lancamento.valorCentavos,
                tipo = lancamento.tipo,
                dataHora = lancamento.dataHora,
                horario = horarioDe(lancamento.dataHora, agora, zona),
                vinculo = rotuloDoVinculo(instrumento, conta),
                categoriaNome = lancamento.categoriaPublicId?.let { categoriasPorId[it]?.nome },
                textoInterpretado = trechoDe(lancamento.textoBruto),
                podeConfirmar = instrumento != null,
            )
        }

    return ReviewUiState(
        carregando = !base.carregado,
        pendentes = pendentes,
        painelDeCategoria = controle.painelDeCategoria,
        painelDeTransferencia = controle.painelDeTransferencia,
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

private const val LIMITE_DO_TRECHO = 160
private val DATA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR"))
private val HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR"))

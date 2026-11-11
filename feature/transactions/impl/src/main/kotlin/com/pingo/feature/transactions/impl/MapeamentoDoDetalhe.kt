package com.pingo.feature.transactions.impl

import com.pingo.core.data.detalhe.MotivoDaRecusa
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento

internal data class BaseDoDetalhe(
    val carregado: Boolean = false,
    val lancamentos: List<Lancamento> = emptyList(),
    val contas: List<Conta> = emptyList(),
    val instrumentos: List<Instrumento> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
)

internal data class OpcoesDoDetalhe(
    val instrumento: OpcaoDeInstrumento?,
    val destino: OpcaoDeInstrumento?,
    val categoria: OpcaoDeCategoria?,
    val instrumentos: List<OpcaoDeInstrumento>,
    val destinos: List<OpcaoDeInstrumento>,
    val categorias: List<OpcaoDeCategoria>,
)

internal fun opcoesDoDetalhe(
    base: BaseDoDetalhe,
    instrumentoPublicId: String?,
    destinoPublicId: String?,
    categoriaPublicId: String?,
    tipo: TipoLancamento,
): OpcoesDoDetalhe {
    val contasPorId = base.contas.associateBy { it.publicId }
    val instrumentosPorId = base.instrumentos.associateBy { it.publicId }
    val ativos = instrumentosAtivos(base)
    val instrumento = instrumentoPublicId?.let { instrumentosPorId[it] }
    val categoria = categoriaPublicId?.let { id -> base.categorias.firstOrNull { candidata -> candidata.publicId == id } }

    return OpcoesDoDetalhe(
        instrumento = instrumento?.let { OpcaoDeInstrumento(it.publicId, rotuloDoInstrumento(contasPorId, it)) },
        destino = destinoPublicId?.let { instrumentosPorId[it] }
            ?.let { OpcaoDeInstrumento(it.publicId, rotuloDoInstrumento(contasPorId, it)) },
        categoria = categoria?.let { OpcaoDeCategoria(it.publicId, it.nome) },
        instrumentos = ativos.map { OpcaoDeInstrumento(it.publicId, rotuloDoInstrumento(contasPorId, it)) },
        destinos = destinos(ativos, instrumento, contasPorId),
        categorias = categoriasPara(base.categorias, tipo),
    )
}

internal fun instrumentosAtivos(base: BaseDoDetalhe): List<Instrumento> =
    base.instrumentos.filter { it.excluidoEm == null && !it.arquivado }

internal fun destinos(
    instrumentos: List<Instrumento>,
    instrumento: Instrumento?,
    contasPorId: Map<String, Conta>,
): List<OpcaoDeInstrumento> {
    val contaAtual = instrumento?.contaPublicId ?: return emptyList()

    return instrumentos
        .filter { it.contaPublicId != contaAtual }
        .map { OpcaoDeInstrumento(it.publicId, rotuloDoInstrumento(contasPorId, it)) }
}

internal fun categoriasPara(categorias: List<Categoria>, tipo: TipoLancamento): List<OpcaoDeCategoria> {
    val desejado = when (tipo) {
        TipoLancamento.DESPESA -> TipoCategoria.DESPESA
        TipoLancamento.RECEITA -> TipoCategoria.RECEITA
        TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> null
    } ?: return emptyList()

    return categorias.filter { it.podeSerEscolhida && it.tipo == desejado }.map { OpcaoDeCategoria(it.publicId, it.nome) }
}

internal fun compativel(categoria: Categoria, tipo: TipoLancamento): Boolean = when (tipo) {
    TipoLancamento.DESPESA -> categoria.tipo == TipoCategoria.DESPESA
    TipoLancamento.RECEITA -> categoria.tipo == TipoCategoria.RECEITA
    TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> true
}

internal fun rotuloDoInstrumento(contasPorId: Map<String, Conta>, instrumento: Instrumento): String =
    rotuloDoInstrumentoDoLancamento(contasPorId.values.toList(), instrumento)

internal fun origemDe(registro: Lancamento): OrigemDoLancamentoUi = if (registro.origem == OrigemLancamento.MANUAL) {
    OrigemDoLancamentoUi.Manual(registradoEm = registro.criadoEm)
} else {
    OrigemDoLancamentoUi.Capturado(
        instituicao = registro.instituicaoOrigem,
        lidoEm = registro.criadoEm,
        textoBruto = registro.textoBruto,
        confianca = registro.confianca,
    )
}

internal fun parDe(registro: Lancamento?, base: BaseDoDetalhe): ParDaTransferenciaUi? {
    val transferenciaId = registro?.transferenciaId ?: return null
    val par = base.lancamentos.firstOrNull { it.transferenciaId == transferenciaId && it.publicId != registro.publicId }

    return par?.let { irmao ->
        val contasPorId = base.contas.associateBy { it.publicId }
        val instrumentosPorId = base.instrumentos.associateBy { it.publicId }
        val instrumento = irmao.instrumentoPublicId?.let { instrumentosPorId[it] }
        val categoria = irmao.categoriaPublicId?.let { id -> base.categorias.firstOrNull { it.publicId == id } }

        ParDaTransferenciaUi(
            publicId = irmao.publicId,
            rotuloDoVinculo = instrumento?.let { rotuloDoInstrumento(contasPorId, it) } ?: "Instrumento não identificado",
            entrada = entradaDe(irmao, categoria),
            valorCentavos = irmao.valorCentavos,
            dataHora = irmao.dataHora,
        )
    }
}

internal fun entradaDe(irmao: Lancamento, categoria: Categoria?): Boolean? = when {
    irmao.tipoSemTransferencia == TipoLancamento.RECEITA -> true
    irmao.tipoSemTransferencia == TipoLancamento.DESPESA -> false
    categoria?.tipo == TipoCategoria.RECEITA || irmao.tipo == TipoLancamento.RECEITA -> true
    categoria?.tipo == TipoCategoria.DESPESA || irmao.tipo == TipoLancamento.DESPESA -> false
    else -> null
}

internal fun irmaoDe(registro: Lancamento, lancamentos: List<Lancamento>): Lancamento? = registro.transferenciaId
    ?.let { id -> lancamentos.firstOrNull { it.transferenciaId == id && it.publicId != registro.publicId } }

internal fun mensagemDaRecusa(motivo: MotivoDaRecusa): MensagemDoDetalhe = when (motivo) {
    MotivoDaRecusa.INSTRUMENTO_NAO_RESOLVIDO -> MensagemDoDetalhe.INSTRUMENTO_PARA_TRANSFERENCIA
    MotivoDaRecusa.DESTINO_NAO_ENCONTRADO -> MensagemDoDetalhe.DESTINO_OBRIGATORIO
    MotivoDaRecusa.MESMA_CONTA -> MensagemDoDetalhe.MESMA_CONTA
    MotivoDaRecusa.PAR_AMBIGUO -> MensagemDoDetalhe.PAR_AMBIGUO
    MotivoDaRecusa.PAR_NAO_RESOLVIDO -> MensagemDoDetalhe.PAR_NAO_RESOLVIDO
    MotivoDaRecusa.JA_E_TRANSFERENCIA -> MensagemDoDetalhe.PAR_AMBIGUO
    MotivoDaRecusa.DIRECAO_DESCONHECIDA -> MensagemDoDetalhe.DIRECAO_DESCONHECIDA
    MotivoDaRecusa.PAR_INCONSISTENTE -> MensagemDoDetalhe.PAR_INCONSISTENTE
}

internal fun tipoDaTela(tipo: TipoLancamento): TipoDoLancamentoManual = when (tipo) {
    TipoLancamento.TRANSFERENCIA -> TipoDoLancamentoManual.TRANSFERENCIA
    TipoLancamento.RECEITA -> TipoDoLancamentoManual.RECEITA
    TipoLancamento.DESPESA, TipoLancamento.AJUSTE -> TipoDoLancamentoManual.DESPESA
}

internal fun tipoDoLancamento(tipo: TipoDoLancamentoManual): TipoLancamento = when (tipo) {
    TipoDoLancamentoManual.DESPESA -> TipoLancamento.DESPESA
    TipoDoLancamentoManual.RECEITA -> TipoLancamento.RECEITA
    TipoDoLancamentoManual.TRANSFERENCIA -> TipoLancamento.TRANSFERENCIA
}

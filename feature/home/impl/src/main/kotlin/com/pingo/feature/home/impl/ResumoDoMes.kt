package com.pingo.feature.home.impl

import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

internal data class ResumoDoMes(val saldoCentavos: Long, val receitasCentavos: Long, val despesasCentavos: Long)

internal fun resumoDoMes(lancamentos: List<Lancamento>, mes: YearMonth, zona: ZoneId): ResumoDoMes {
    val doMes = lancamentos.filter { registradoEm(it, mes, zona) }
    val receitas = doMes.filter { it.tipo == TipoLancamento.RECEITA }.sumOf { it.valorCentavos }
    val despesas = doMes.filter { it.tipo == TipoLancamento.DESPESA }.sumOf { it.valorCentavos }

    return ResumoDoMes(saldoCentavos = receitas - despesas, receitasCentavos = receitas, despesasCentavos = despesas)
}

internal fun categoriasDoTopo(
    lancamentos: List<Lancamento>,
    mes: YearMonth,
    zona: ZoneId,
    categoriasPorId: Map<String, Categoria>,
): List<CategoriaDoTopo> {
    val despesas = lancamentos.filter { registradoEm(it, mes, zona) && it.tipo == TipoLancamento.DESPESA }
    val total = despesas.sumOf { it.valorCentavos }

    if (total <= 0) {
        return emptyList()
    }

    return despesas
        .mapNotNull { lancamento ->
            val categoria = lancamento.categoriaPublicId?.let(categoriasPorId::get) ?: return@mapNotNull null
            categoria to lancamento.valorCentavos
        }
        .groupBy({ (categoria, _) -> categoria.publicId }, { (_, valor) -> valor })
        .mapNotNull { (publicId, valores) ->
            val categoria = categoriasPorId[publicId] ?: return@mapNotNull null
            val valor = valores.sum()

            CategoriaDoTopo(
                publicId = categoria.publicId,
                nome = categoria.nome,
                valorCentavos = valor,
                percentual = ((valor * 100.0) / total).roundToInt(),
            )
        }
        .sortedByDescending { it.valorCentavos }
        .take(TOP_CATEGORIAS)
}

internal fun ultimosLancamentos(
    lancamentos: List<Lancamento>,
    categoriasPorId: Map<String, Categoria>,
    instrumentosPorId: Map<String, Instrumento>,
    contasPorId: Map<String, Conta>,
): List<LancamentoDaInicio> = lancamentos
    .filter { it.excluidoEm == null }
    .sortedByDescending { it.dataHora }
    .take(ULTIMOS_LANCAMENTOS)
    .map { lancamento ->
        val instrumento = lancamento.instrumentoPublicId?.let(instrumentosPorId::get)

        LancamentoDaInicio(
            lancamento = lancamento,
            categoria = lancamento.categoriaPublicId?.let(categoriasPorId::get),
            instrumento = instrumento,
            conta = instrumento?.let { contasPorId[it.contaPublicId] },
        )
    }

internal fun contarPendentes(lancamentos: List<Lancamento>): Int = lancamentos.count {
    it.status == StatusLancamento.PENDENTE && it.excluidoEm == null
}

internal fun mesesDosLancamentos(lancamentos: List<Lancamento>, zona: ZoneId): List<YearMonth> = lancamentos
    .filter { it.excluidoEm == null }
    .map { YearMonth.from(it.dataHora.atZone(zona)) }
    .distinct()
    .sorted()

private fun registradoEm(lancamento: Lancamento, mes: YearMonth, zona: ZoneId): Boolean = lancamento.excluidoEm == null &&
    YearMonth.from(lancamento.dataHora.atZone(zona)) == mes

private const val TOP_CATEGORIAS = 3
private const val ULTIMOS_LANCAMENTOS = 5

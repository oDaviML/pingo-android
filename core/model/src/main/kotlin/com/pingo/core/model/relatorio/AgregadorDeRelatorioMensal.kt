package com.pingo.core.model.relatorio

import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

object AgregadorDeRelatorioMensal {
    val ZONA_SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")
    private const val QUANTIDADE_MESES_SERIE = 6
    private const val SOMA_PERCENTUAL_INTEIRO = 100
    private const val SOMA_PONTOS_BASE = 10_000

    fun intervaloDoMes(mes: YearMonth): Pair<Instant, Instant> {
        val inicioInclusivo = mes.atDay(1).atStartOfDay(ZONA_SAO_PAULO).toInstant()
        val fimExclusivo = mes.plusMonths(1).atDay(1).atStartOfDay(ZONA_SAO_PAULO).toInstant()
        return inicioInclusivo to fimExclusivo
    }

    fun elegivelParaRelatorio(lancamento: Lancamento): Boolean = lancamento.excluidoEm == null &&
        lancamento.status == StatusLancamento.CONFIRMADO &&
        (lancamento.tipo == TipoLancamento.RECEITA || lancamento.tipo == TipoLancamento.DESPESA)

    fun pertenceAoMes(dataHora: Instant, mes: YearMonth): Boolean {
        val (inicio, fimExclusivo) = intervaloDoMes(mes)
        return !dataHora.isBefore(inicio) && dataHora.isBefore(fimExclusivo)
    }

    fun calcularVariacao(atualCentavos: Long, anteriorCentavos: Long): VariacaoCategoria = if (anteriorCentavos <= 0L) {
        VariacaoCategoria.SemBase
    } else {
        val percentual = ((atualCentavos - anteriorCentavos) * 100.0 / anteriorCentavos).roundToInt()
        VariacaoCategoria.Percentual(percentual)
    }

    fun distribuirPartesInteiras(valores: List<Long>, somaAlvo: Int): List<Int> {
        if (valores.isEmpty()) {
            return emptyList()
        }
        val total = valores.sum()
        if (total <= 0L) {
            return List(valores.size) { 0 }
        }
        val bases = valores.map { ((it * somaAlvo) / total).toInt() }.toMutableList()
        val restos = valores.map { (it * somaAlvo) % total }
        val faltantes = somaAlvo - bases.sum()
        val indicesOrdenados = valores.indices.sortedWith(
            compareByDescending<Int> { restos[it] }
                .thenByDescending { valores[it] }
                .thenBy { it },
        )
        for (k in 0 until faltantes) {
            val indice = indicesOrdenados[k]
            bases[indice] = bases[indice] + 1
        }
        return bases
    }

    fun agregar(mes: YearMonth, lancamentos: List<Lancamento>): RelatorioMensal {
        val elegiveis = lancamentos.filter { elegivelParaRelatorio(it) }
        val doMes = elegiveis.filter { pertenceAoMes(it.dataHora, mes) }
        val doMesAnterior = elegiveis.filter { pertenceAoMes(it.dataHora, mes.minusMonths(1)) }

        val totalReceitas = doMes.filter { it.tipo == TipoLancamento.RECEITA }.sumOf { it.valorCentavos }
        val totalDespesas = doMes.filter { it.tipo == TipoLancamento.DESPESA }.sumOf { it.valorCentavos }

        val categoriasDespesa = agregarCategoriasDespesa(doMes, doMesAnterior)
        val serieSeisMeses = montarSerieSeisMeses(mes, elegiveis)

        return RelatorioMensal(
            mes = mes,
            totalReceitasCentavos = totalReceitas,
            totalDespesasCentavos = totalDespesas,
            saldoMensalCentavos = totalReceitas - totalDespesas,
            categoriasDespesa = categoriasDespesa,
            serieSeisMeses = serieSeisMeses,
        )
    }

    private fun agregarCategoriasDespesa(doMes: List<Lancamento>, doMesAnterior: List<Lancamento>): List<ResumoCategoriaMensal> {
        val despesasMes = doMes.filter { it.tipo == TipoLancamento.DESPESA }
        val despesasMesAnteriorPorCategoria = doMesAnterior
            .filter { it.tipo == TipoLancamento.DESPESA }
            .groupBy { it.categoriaPublicId }
            .mapValues { (_, itens) -> itens.sumOf { it.valorCentavos } }

        val grupos = despesasMes
            .groupBy { it.categoriaPublicId }
            .map { (categoriaId, itens) -> categoriaId to itens.sumOf { it.valorCentavos } }
            .filter { (_, valor) -> valor > 0L }
            .sortedWith(compareByDescending<Pair<String?, Long>> { it.second }.thenBy { it.first ?: "" })

        val valores = grupos.map { it.second }
        val percentuais = distribuirPartesInteiras(valores, SOMA_PERCENTUAL_INTEIRO)
        val pontosBase = distribuirPartesInteiras(valores, SOMA_PONTOS_BASE)

        return grupos.mapIndexed { index, (categoriaId, valorAtual) ->
            val valorAnterior = despesasMesAnteriorPorCategoria[categoriaId] ?: 0L
            ResumoCategoriaMensal(
                categoriaPublicId = categoriaId,
                valorCentavos = valorAtual,
                valorMesAnteriorCentavos = valorAnterior,
                percentualInteiro = percentuais[index],
                pontosBase = pontosBase[index],
                variacaoMesAnterior = calcularVariacao(valorAtual, valorAnterior),
            )
        }
    }

    private fun montarSerieSeisMeses(mesFinal: YearMonth, elegiveis: List<Lancamento>): List<PontoSerieMensal> =
        ((QUANTIDADE_MESES_SERIE - 1) downTo 0).map { deslocamento ->
            val mes = mesFinal.minusMonths(deslocamento.toLong())
            val doMes = elegiveis.filter { pertenceAoMes(it.dataHora, mes) }
            val receitas = doMes.filter { it.tipo == TipoLancamento.RECEITA }.sumOf { it.valorCentavos }
            val despesas = doMes.filter { it.tipo == TipoLancamento.DESPESA }.sumOf { it.valorCentavos }
            PontoSerieMensal(
                mes = mes,
                receitasCentavos = receitas,
                despesasCentavos = despesas,
            )
        }
}

package com.pingo.feature.transactions.impl

import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

internal object ListaDeLancamentos {
    private val MARCAS = Regex("\\p{M}+")

    fun filtrar(
        lancamentos: List<Lancamento>,
        busca: String,
        filtros: FiltrosDeLancamentos,
        instrumentosPorId: Map<String, Instrumento>,
        zona: ZoneId,
    ): List<Lancamento> {
        val termo = normalizar(busca)

        return lancamentos.filter { lancamento ->
            combinaMes(lancamento, filtros.mes, zona) &&
                combinaBusca(lancamento, termo) &&
                combinaTipo(lancamento, filtros.tipo) &&
                combinaVinculo(lancamento, filtros.vinculo, instrumentosPorId) &&
                combinaCategoria(lancamento, filtros.categoriaPublicId) &&
                combinaStatus(lancamento, filtros.status)
        }
    }

    fun agrupar(
        lancamentos: List<Lancamento>,
        categoriasPorId: Map<String, Categoria>,
        instrumentosPorId: Map<String, Instrumento>,
        contasPorId: Map<String, Conta>,
        hoje: LocalDate,
        zona: ZoneId,
    ): List<DiaDeLancamentos> = lancamentos
        .sortedByDescending { it.dataHora }
        .groupBy { it.dataHora.atZone(zona).toLocalDate() }
        .map { (data, doDia) ->
            DiaDeLancamentos(
                data = data,
                rotulo = RotuloDeDia.de(data, hoje),
                totalCentavos = doDia.sumOf(::valorComSinal),
                itens = doDia.map { lancamento ->
                    LancamentoDaLista(
                        lancamento = lancamento,
                        categoria = lancamento.categoriaPublicId?.let(categoriasPorId::get),
                        instrumento = lancamento.instrumentoPublicId?.let(instrumentosPorId::get),
                        conta = lancamento.instrumentoPublicId
                            ?.let(instrumentosPorId::get)
                            ?.let { instrumento -> contasPorId[instrumento.contaPublicId] },
                    )
                },
            )
        }

    fun valorComSinal(lancamento: Lancamento): Long = when (lancamento.tipo) {
        TipoLancamento.DESPESA -> -lancamento.valorCentavos
        TipoLancamento.RECEITA -> lancamento.valorCentavos
        TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> 0
    }

    private fun combinaMes(lancamento: Lancamento, mes: YearMonth?, zona: ZoneId): Boolean =
        mes == null || YearMonth.from(lancamento.dataHora.atZone(zona)) == mes

    private fun combinaBusca(lancamento: Lancamento, termo: String): Boolean {
        if (termo.isEmpty()) {
            return true
        }

        val estabelecimento = lancamento.estabelecimento?.let(::normalizar).orEmpty()
        return estabelecimento.contains(termo)
    }

    private fun combinaTipo(lancamento: Lancamento, tipo: TipoLancamento?): Boolean = tipo == null || lancamento.tipo == tipo

    private fun combinaVinculo(lancamento: Lancamento, vinculo: VinculoFiltro?, instrumentosPorId: Map<String, Instrumento>): Boolean {
        val instrumento = lancamento.instrumentoPublicId?.let(instrumentosPorId::get) ?: return vinculo == null

        return when (vinculo) {
            null -> true
            is VinculoFiltro.Instrumento -> instrumento.publicId == vinculo.publicId
            is VinculoFiltro.Conta -> instrumento.contaPublicId == vinculo.publicId
        }
    }

    private fun combinaCategoria(lancamento: Lancamento, categoriaPublicId: String?): Boolean =
        categoriaPublicId == null || lancamento.categoriaPublicId == categoriaPublicId

    private fun combinaStatus(lancamento: Lancamento, status: StatusFiltro?): Boolean = when (status) {
        null -> true
        StatusFiltro.PENDENTE -> lancamento.status == StatusLancamento.PENDENTE
        StatusFiltro.CONFIRMADO -> lancamento.status == StatusLancamento.CONFIRMADO
        StatusFiltro.MANUAL -> lancamento.origem == OrigemLancamento.MANUAL
    }

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD).replace(MARCAS, "").lowercase(Locale("pt", "BR")).trim()
}

package com.pingo.feature.accounts.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.model.Categoria
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.fatura.CicloDeFaturaDetalhado
import java.time.LocalDate
import java.time.ZoneId

enum class AbaFaturas(val indice: Int, val rotulo: String) {
    ABERTA(indice = 0, rotulo = "Aberta"),
    PROXIMAS(indice = 1, rotulo = "Próximas"),
    FECHADAS(indice = 2, rotulo = "Fechadas"),
    ;

    companion object {
        fun porIndice(indice: Int): AbaFaturas = entries.getOrElse(indice) { ABERTA }
    }
}

@Immutable
data class LancamentoDaFaturaUi(val lancamento: Lancamento, val categoria: Categoria?)

@Immutable
data class FaturasUiState(
    val carregando: Boolean = true,
    val falha: Boolean = false,
    val naoEncontrada: Boolean = false,
    val instrumentoPublicId: String = "",
    val tituloCartao: String = "Faturas",
    val instituicao: InstituicaoFinanceira = InstituicaoFinanceira.OUTRA,
    val ultimosDigitos: String? = null,
    val configuracaoIncompleta: Boolean = false,
    val abaSelecionada: AbaFaturas = AbaFaturas.ABERTA,
    val ciclosAbertos: List<CicloDeFaturaDetalhado> = emptyList(),
    val ciclosProximos: List<CicloDeFaturaDetalhado> = emptyList(),
    val ciclosFechados: List<CicloDeFaturaDetalhado> = emptyList(),
    val competenciaSelecionadaProximas: LocalDate? = null,
    val competenciaSelecionadaFechadas: LocalDate? = null,
    val categoriasPorId: Map<String, Categoria> = emptyMap(),
    val hoje: LocalDate = LocalDate.now(),
    val zona: ZoneId = ZoneId.systemDefault(),
    val modalCompletarDatas: ModalCompletarDatasUiState? = null,
) {
    val ciclosDaAbaAtual: List<CicloDeFaturaDetalhado>
        get() = when (abaSelecionada) {
            AbaFaturas.ABERTA -> ciclosAbertos
            AbaFaturas.PROXIMAS -> ciclosProximos
            AbaFaturas.FECHADAS -> ciclosFechados
        }

    val cicloSelecionado: CicloDeFaturaDetalhado?
        get() = when (abaSelecionada) {
            AbaFaturas.ABERTA -> ciclosAbertos.firstOrNull()
            AbaFaturas.PROXIMAS -> ciclosProximos.firstOrNull { it.competencia == competenciaSelecionadaProximas }
                ?: ciclosProximos.firstOrNull()
            AbaFaturas.FECHADAS -> ciclosFechados.firstOrNull { it.competencia == competenciaSelecionadaFechadas }
                ?: ciclosFechados.firstOrNull()
        }

    val lancamentosDoCicloSelecionado: List<LancamentoDaFaturaUi>
        get() = cicloSelecionado?.lancamentos.orEmpty().map { lancamento ->
            LancamentoDaFaturaUi(
                lancamento = lancamento,
                categoria = lancamento.categoriaPublicId?.let { categoriasPorId[it] },
            )
        }
}

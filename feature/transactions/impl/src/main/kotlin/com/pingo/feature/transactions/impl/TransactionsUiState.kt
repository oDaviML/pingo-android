package com.pingo.feature.transactions.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.TipoLancamento
import java.time.LocalDate
import java.time.YearMonth

@Immutable
data class TransactionsUiState(
    val carregando: Boolean = true,
    val busca: String = "",
    val filtros: FiltrosDeLancamentos = FiltrosDeLancamentos(),
    val filtrosVisiveis: Boolean = false,
    val mesesDisponiveis: List<YearMonth> = emptyList(),
    val opcoesDeVinculo: List<OpcaoDeVinculo> = emptyList(),
    val opcoesDeCategoria: List<OpcaoDeCategoria> = emptyList(),
    val dias: List<DiaDeLancamentos> = emptyList(),
    val quantidadeDeLancamentos: Int = 0,
    val filtrosAtivos: Int = 0,
    val estadoVazio: EstadoVazioDosLancamentos? = null,
    val mensagemDeExclusao: MensagemDeExclusao? = null,
    val trocaDeCategoria: TrocaDeCategoria? = null,
    val exportacaoCsvPronta: ExportacaoCsvPronta? = null,
    val avisoExportacaoCsv: String? = null,
)

@Immutable
data class FiltrosDeLancamentos(
    val mes: YearMonth? = null,
    val tipo: TipoLancamento? = null,
    val vinculo: VinculoFiltro? = null,
    val categoriaPublicId: String? = null,
    val status: StatusFiltro? = null,
) {
    val ativos: Int
        get() = listOfNotNull(tipo, vinculo, categoriaPublicId, status).size
}

@Immutable
sealed interface VinculoFiltro {
    val publicId: String

    @Immutable
    data class Conta(override val publicId: String) : VinculoFiltro

    @Immutable
    data class Instrumento(override val publicId: String) : VinculoFiltro
}

enum class StatusFiltro {
    PENDENTE,
    CONFIRMADO,
    MANUAL,
}

@Immutable
data class OpcaoDeVinculo(val filtro: VinculoFiltro, val rotulo: String, val chave: String)

@Immutable
data class OpcaoDeCategoria(val publicId: String, val nome: String)

@Immutable
data class DiaDeLancamentos(val data: LocalDate, val rotulo: String, val totalCentavos: Long, val itens: List<LancamentoDaLista>)

@Immutable
data class LancamentoDaLista(val lancamento: Lancamento, val categoria: Categoria?, val instrumento: Instrumento?, val conta: Conta?)

@Immutable
data class MensagemDeExclusao(val publicId: String)

@Immutable
data class TrocaDeCategoria(
    val lancamentoPublicId: String,
    val estabelecimento: String,
    val categoriaAtualPublicId: String?,
    val opcoes: List<OpcaoDeCategoria>,
)

@Immutable
data class ExportacaoCsvPronta(val conteudoCsv: String, val nomeArquivo: String = "pingo-lancamentos.csv")

enum class EstadoVazioDosLancamentos {
    SEM_LANCAMENTOS,
    NADA_COM_OS_FILTROS,
}

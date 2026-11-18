package com.pingo.feature.reports.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.designsystem.component.chart.PingoComparisonRowUi
import com.pingo.core.designsystem.component.chart.PingoDonutSlice
import com.pingo.core.designsystem.component.chart.PingoIncomeExpensePoint
import com.pingo.core.designsystem.component.chart.PingoSubscriptionRowUi
import com.pingo.core.designsystem.component.chart.PingoSubscriptionSuggestionUi
import com.pingo.core.model.assinatura.ChaveAssinatura
import java.time.YearMonth

@Immutable
data class ReportsUiState(
    val carregando: Boolean = true,
    val falha: Boolean = false,
    val mes: YearMonth = YearMonth.now(),
    val rotuloMes: String = "",
    val podeAvancarMes: Boolean = false,
    val mesVazio: Boolean = false,
    val rotuloMesAnterior: String = "",
    val totalDespesasCentavos: Long = 0L,
    val totalDespesasFormatado: String = "R$ 0,00",
    val totalReceitasCentavos: Long = 0L,
    val saldoMensalCentavos: Long = 0L,
    val fatiasDonut: List<PingoDonutSlice> = emptyList(),
    val categorias: List<CategoriaRelatorioUi> = emptyList(),
    val comparativoGeral: ComparativoCategoriaUi? = null,
    val comparativos: List<ComparativoCategoriaUi> = emptyList(),
    val serieSeisMeses: List<PontoSerieUi> = emptyList(),
    val sugestoesAssinatura: List<SugestaoAssinaturaUi> = emptyList(),
    val assinaturasConfirmadas: List<AssinaturaConfirmadaUi> = emptyList(),
    val totalMensalAssinaturasCentavos: Long = 0L,
    val totalMensalAssinaturasFormatado: String = "R$ 0,00",
    val assinaturasEmProcessamento: Set<String> = emptySet(),
    val confirmacaoCancelamento: AssinaturaConfirmadaUi? = null,
    val mensagemErroAcao: String? = null,
)

@Immutable
data class CategoriaRelatorioUi(
    val categoriaPublicId: String?,
    val nome: String,
    val valorCentavos: Long,
    val valorFormatado: String,
    val percentualInteiro: Int,
    val subtitulo: String,
    val acessibilidade: String,
)

@Immutable
data class ComparativoCategoriaUi(val categoriaPublicId: String?, val row: PingoComparisonRowUi)

@Immutable
data class PontoSerieUi(val mes: YearMonth, val point: PingoIncomeExpensePoint)

@Immutable
data class SugestaoAssinaturaUi(val chave: ChaveAssinatura, val chaveTexto: String, val card: PingoSubscriptionSuggestionUi)

@Immutable
data class AssinaturaConfirmadaUi(val publicId: String, val estabelecimento: String, val row: PingoSubscriptionRowUi)

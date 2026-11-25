package com.pingo.feature.reports.impl

import com.pingo.core.designsystem.component.chart.PingoComparisonRowUi
import com.pingo.core.designsystem.component.chart.PingoComparisonTrend
import com.pingo.core.designsystem.component.chart.PingoDonutSlice
import com.pingo.core.designsystem.component.chart.PingoIncomeExpensePoint
import com.pingo.core.designsystem.component.chart.PingoSubscriptionRowUi
import com.pingo.core.designsystem.component.chart.PingoSubscriptionSuggestionUi
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.model.Assinatura
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.assinatura.ChaveAssinatura
import com.pingo.core.model.assinatura.DetectorDeAssinaturas
import com.pingo.core.model.assinatura.SugestaoDeAssinatura
import com.pingo.core.model.relatorio.AgregadorDeRelatorioMensal
import com.pingo.core.model.relatorio.RelatorioMensalDetalhado
import com.pingo.core.model.relatorio.ResumoCategoriaRelatorio
import com.pingo.core.model.relatorio.VariacaoCategoria
import com.pingo.core.ui.Valores
import com.pingo.core.ui.iconeDaCategoria
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

internal const val SEM_CATEGORIA_SLICE_ID = "__sem_categoria__"

private val LOCALE_PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val FORMATO_MES_ANO: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_PT_BR)
private val FORMATO_MES_NOME: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM", LOCALE_PT_BR)
private val FORMATO_MES_ABREV: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", LOCALE_PT_BR)
private val FORMATO_DIA_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM", LOCALE_PT_BR)

internal fun chaveTextoDe(chave: ChaveAssinatura): String = "${chave.instrumentoPublicId}|${chave.estabelecimentoNormalizado}"

internal fun rotuloMesCompleto(mes: YearMonth): String =
    FORMATO_MES_ANO.format(mes).replaceFirstChar { letra -> if (letra.isLowerCase()) letra.titlecase(LOCALE_PT_BR) else letra.toString() }

internal fun nomeMesMinusculo(mes: YearMonth): String = FORMATO_MES_NOME.format(mes).lowercase(LOCALE_PT_BR)

internal fun mesAbreviado(mes: YearMonth): String = FORMATO_MES_ABREV.format(mes).removeSuffix(".").lowercase(LOCALE_PT_BR)

internal fun formatarDiaMes(data: LocalDate): String = FORMATO_DIA_MES.format(data)

@Suppress("LongParameterList")
internal fun mapearEstadoRelatorio(
    mes: YearMonth,
    mesAtual: YearMonth,
    detalhado: RelatorioMensalDetalhado,
    confirmadas: List<Assinatura>,
    sugestoes: List<SugestaoDeAssinatura>,
    contas: List<Conta>,
    instrumentos: List<Instrumento>,
): ReportsUiState {
    val contasPorId = contas.associateBy { it.publicId }
    val instrumentosPorId = instrumentos.associateBy { it.publicId }
    val relatorio = detalhado.relatorio
    val mesAnterior = mes.minusMonths(1)
    val mesAnteriorAbrev = mesAbreviado(mesAnterior)
    val totalDespesas = relatorio.totalDespesasCentavos
    val despesasMesAnterior = relatorio.serieSeisMeses.firstOrNull { it.mes == mesAnterior }?.despesasCentavos ?: 0L
    val confirmadasUi = mapearConfirmadas(confirmadas, instrumentosPorId, contasPorId)
    val totalMensalAssinaturas = DetectorDeAssinaturas.totalMensalConfirmadasCentavos(confirmadas)

    return ReportsUiState(
        carregando = false,
        falha = false,
        mes = mes,
        rotuloMes = rotuloMesCompleto(mes),
        podeAvancarMes = mes < mesAtual,
        mesVazio = totalDespesas == 0L && relatorio.totalReceitasCentavos == 0L,
        rotuloMesAnterior = nomeMesMinusculo(mesAnterior),
        totalDespesasCentavos = totalDespesas,
        totalDespesasFormatado = Valores.formatar(totalDespesas),
        totalReceitasCentavos = relatorio.totalReceitasCentavos,
        saldoMensalCentavos = relatorio.saldoMensalCentavos,
        fatiasDonut = mapearFatiasDonut(detalhado.categorias),
        categorias = detalhado.categorias.map { mapearCategoria(it, mesAnteriorAbrev) },
        comparativoGeral = comparativoGeralDe(totalDespesas, despesasMesAnterior, mesAnteriorAbrev),
        comparativos = detalhado.categorias.map { mapearComparativoCategoria(it, mesAnteriorAbrev) },
        serieSeisMeses = mapearSerieSeisMeses(mes, relatorio.serieSeisMeses),
        sugestoesAssinatura = sugestoes.map { mapearSugestao(it, instrumentosPorId, contasPorId) },
        assinaturasConfirmadas = confirmadasUi,
        totalMensalAssinaturasCentavos = totalMensalAssinaturas,
        totalMensalAssinaturasFormatado = Valores.formatar(totalMensalAssinaturas),
    )
}

private fun mapearFatiasDonut(categorias: List<ResumoCategoriaRelatorio>): List<PingoDonutSlice> = categorias.map { categoria ->
    PingoDonutSlice(
        id = categoria.categoriaPublicId ?: SEM_CATEGORIA_SLICE_ID,
        label = categoria.nomeCategoria,
        amount = categoria.valorCentavos.toDouble(),
        percentage = categoria.percentualInteiro,
    )
}

private fun comparativoGeralDe(totalDespesas: Long, despesasMesAnterior: Long, mesAnteriorAbrev: String): ComparativoCategoriaUi? =
    if (totalDespesas > 0L || despesasMesAnterior > 0L) {
        mapearComparativoGeral(totalDespesas, despesasMesAnterior, mesAnteriorAbrev)
    } else {
        null
    }

private fun mapearSerieSeisMeses(
    mesSelecionado: YearMonth,
    serie: List<com.pingo.core.model.relatorio.PontoSerieMensal>,
): List<PontoSerieUi> = serie.map { ponto ->
    PontoSerieUi(
        mes = ponto.mes,
        point = PingoIncomeExpensePoint(
            monthLabel = mesAbreviado(ponto.mes),
            income = ponto.receitasCentavos.toDouble(),
            expense = ponto.despesasCentavos.toDouble(),
            incomeFormatted = Valores.formatar(ponto.receitasCentavos),
            expenseFormatted = Valores.formatar(ponto.despesasCentavos),
            isHighlighted = ponto.mes == mesSelecionado,
        ),
    )
}

private fun mapearConfirmadas(
    confirmadas: List<Assinatura>,
    instrumentosPorId: Map<String, Instrumento>,
    contasPorId: Map<String, Conta>,
): List<AssinaturaConfirmadaUi> = confirmadas
    .sortedWith(compareBy<Assinatura> { it.estabelecimento.lowercase(LOCALE_PT_BR) }.thenBy { it.publicId })
    .map { mapearAssinaturaConfirmada(it, instrumentosPorId, contasPorId) }

private fun mapearCategoria(categoria: ResumoCategoriaRelatorio, mesAnteriorAbrev: String): CategoriaRelatorioUi {
    val valorFormatado = Valores.formatar(categoria.valorCentavos)
    val variacaoCurta = formatarVariacaoCurta(categoria.variacaoMesAnterior, mesAnteriorAbrev)
    return CategoriaRelatorioUi(
        categoriaPublicId = categoria.categoriaPublicId,
        nome = categoria.nomeCategoria,
        valorCentavos = categoria.valorCentavos,
        valorFormatado = valorFormatado,
        percentualInteiro = categoria.percentualInteiro,
        subtitulo = "${categoria.percentualInteiro}% · $variacaoCurta",
        acessibilidade = "${categoria.nomeCategoria}, $valorFormatado, ${categoria.percentualInteiro}% das despesas",
    )
}

private fun mapearComparativoGeral(atualCentavos: Long, anteriorCentavos: Long, mesAnteriorAbrev: String): ComparativoCategoriaUi {
    val variacao = AgregadorDeRelatorioMensal.calcularVariacao(atualCentavos, anteriorCentavos)
    val (trend, badge) = converterVariacaoParaBadge(variacao)
    val subtitulo = "${Valores.formatar(atualCentavos)} · ${formatarDeltaBrl(atualCentavos, anteriorCentavos, mesAnteriorAbrev)}"
    return ComparativoCategoriaUi(
        categoriaPublicId = null,
        row = PingoComparisonRowUi(
            icon = PingoIcons.PieChart,
            categoryName = "Despesas totais",
            subtitle = subtitulo,
            trend = trend,
            badgeText = badge,
        ),
    )
}

private fun mapearComparativoCategoria(categoria: ResumoCategoriaRelatorio, mesAnteriorAbrev: String): ComparativoCategoriaUi {
    val (trend, badge) = converterVariacaoParaBadge(categoria.variacaoMesAnterior)
    val deltaTexto = formatarDeltaBrl(categoria.valorCentavos, categoria.valorMesAnteriorCentavos, mesAnteriorAbrev)
    val subtitulo = "${Valores.formatar(categoria.valorCentavos)} · $deltaTexto"
    return ComparativoCategoriaUi(
        categoriaPublicId = categoria.categoriaPublicId,
        row = PingoComparisonRowUi(
            icon = iconeDaCategoria(categoria.nomeCategoria),
            categoryName = categoria.nomeCategoria,
            subtitle = subtitulo,
            trend = trend,
            badgeText = badge,
        ),
    )
}

private fun converterVariacaoParaBadge(variacao: VariacaoCategoria): Pair<PingoComparisonTrend, String> = when (variacao) {
    VariacaoCategoria.SemBase -> PingoComparisonTrend.NoBase to "sem base"
    is VariacaoCategoria.Percentual -> when {
        variacao.deltaPercentual > 0 -> PingoComparisonTrend.Up to "${variacao.deltaPercentual}%"
        variacao.deltaPercentual < 0 -> PingoComparisonTrend.Down to "${abs(variacao.deltaPercentual)}%"
        else -> PingoComparisonTrend.Equal to "0%"
    }
}

private fun formatarDeltaBrl(atualCentavos: Long, anteriorCentavos: Long, mesAnteriorAbrev: String): String {
    if (anteriorCentavos <= 0L) {
        return "+${Valores.formatar(atualCentavos)} vs $mesAnteriorAbrev"
    }
    val delta = atualCentavos - anteriorCentavos
    return when {
        delta > 0L -> "+${Valores.formatar(delta)} vs $mesAnteriorAbrev"
        delta < 0L -> "-${Valores.formatar(-delta)} vs $mesAnteriorAbrev"
        else -> "R$ 0,00 vs $mesAnteriorAbrev"
    }
}

private fun formatarVariacaoCurta(variacao: VariacaoCategoria, mesAnteriorAbrev: String): String = when (variacao) {
    VariacaoCategoria.SemBase -> "sem base"
    is VariacaoCategoria.Percentual -> when {
        variacao.deltaPercentual > 0 -> "↑ ${variacao.deltaPercentual}% vs $mesAnteriorAbrev"
        variacao.deltaPercentual < 0 -> "↓ ${abs(variacao.deltaPercentual)}% vs $mesAnteriorAbrev"
        else -> "= $mesAnteriorAbrev"
    }
}

private fun mapearSugestao(
    sugestao: SugestaoDeAssinatura,
    instrumentosPorId: Map<String, Instrumento>,
    contasPorId: Map<String, Conta>,
): SugestaoAssinaturaUi {
    val rotuloInstrumento = rotuloDoInstrumento(
        instrumentoPublicId = sugestao.chave.instrumentoPublicId,
        instrumentosPorId = instrumentosPorId,
        contasPorId = contasPorId,
    )
    val proxima = formatarDiaMes(sugestao.proximaCobrancaEstimada)
    return SugestaoAssinaturaUi(
        chave = sugestao.chave,
        chaveTexto = chaveTextoDe(sugestao.chave),
        card = PingoSubscriptionSuggestionUi(
            merchant = sugestao.estabelecimentoExibicao,
            instrumentName = rotuloInstrumento,
            amountFormatted = Valores.formatar(sugestao.valorReferenciaCentavos),
            tagLabel = if (sugestao.porCategoria) "Categoria Assinaturas · Mensal" else "Parece assinatura · Mensal",
            nextChargeLabel = "Próxima cobrança: $proxima",
        ),
    )
}

private fun mapearAssinaturaConfirmada(
    assinatura: Assinatura,
    instrumentosPorId: Map<String, Instrumento>,
    contasPorId: Map<String, Conta>,
): AssinaturaConfirmadaUi {
    val rotuloInstrumento = rotuloDoInstrumento(
        instrumentoPublicId = assinatura.instrumentoPublicId,
        instrumentosPorId = instrumentosPorId,
        contasPorId = contasPorId,
    )
    val nextChargeLabel = assinatura.proximaCobrancaEstimada?.let { data ->
        "Próxima cobrança: ${formatarDiaMes(data)}"
    }
    return AssinaturaConfirmadaUi(
        publicId = assinatura.publicId,
        estabelecimento = assinatura.estabelecimento,
        row = PingoSubscriptionRowUi(
            title = assinatura.estabelecimento,
            subtitle = rotuloInstrumento,
            amountFormatted = Valores.formatar(assinatura.valorReferenciaCentavos),
            nextChargeLabel = nextChargeLabel,
        ),
    )
}

internal fun rotuloDoInstrumento(
    instrumentoPublicId: String?,
    instrumentosPorId: Map<String, Instrumento>,
    contasPorId: Map<String, Conta>,
): String {
    val instrumento = instrumentoPublicId?.let(instrumentosPorId::get) ?: return "Instrumento não identificado"
    val conta = contasPorId[instrumento.contaPublicId]
    val tipo = when (instrumento.tipo) {
        TipoInstrumento.CREDITO -> instrumento.apelido.ifBlank { "Crédito" }
        TipoInstrumento.DEBITO -> instrumento.apelido.ifBlank { "Débito" }
        TipoInstrumento.VOUCHER -> instrumento.beneficio?.name ?: instrumento.apelido.ifBlank { "Voucher" }
    }
    val digitos = instrumento.ultimosDigitos?.takeIf { it.isNotBlank() }
    val base = listOfNotNull(conta?.apelido?.takeIf { it.isNotBlank() }, tipo).distinct().joinToString(" · ")
    return if (digitos != null) "$base $digitos" else base
}

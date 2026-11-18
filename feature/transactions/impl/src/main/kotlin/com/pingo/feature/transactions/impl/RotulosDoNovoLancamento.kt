package com.pingo.feature.transactions.impl

import com.pingo.core.data.manual.CampoDeLancamentoManual
import com.pingo.core.model.fatura.ModoParcelamento
import com.pingo.core.ui.Valores
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object RotulosDoNovoLancamento {
    private val LOCALE = Locale("pt", "BR")
    private val DIA_NO_MES: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE)
    private val DIA_COM_SEMANA: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d 'de' MMMM", LOCALE)
    private val DIA_CURTO: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", LOCALE)
    private val MES: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE)

    val diasDaSemana: List<String> = listOf("D", "S", "T", "Q", "Q", "S", "S")

    fun data(data: LocalDate, hoje: LocalDate): String = when (data) {
        hoje -> "Hoje, ${data.format(DIA_NO_MES)}"
        hoje.minusDays(1) -> "Ontem, ${data.format(DIA_NO_MES)}"
        else -> capitalizado(data.format(DIA_COM_SEMANA))
    }

    fun dataDoCalendario(data: LocalDate): String = capitalizado(data.format(DIA_CURTO))

    fun mes(mes: YearMonth): String = capitalizado(mes.format(MES))

    fun valor(estado: NovoLancamentoUiState): String {
        val parcelaPreview = estado.valorParcelaPreviewCentavos
        val totalPreview = estado.valorTotalPreviewCentavos

        return when {
            CampoDeLancamentoManual.VALOR in estado.erros && estado.valorCentavos > 0L && estado.parcelado ->
                "Valor insuficiente para ${estado.totalParcelas} parcelas"
            CampoDeLancamentoManual.VALOR in estado.erros -> "O valor precisa ser maior que zero"
            origemIgualAoDestino(estado) -> "Origem e destino precisam ser diferentes"
            parcelaPreview != null && totalPreview != null ->
                "${estado.totalParcelas}x de ${Valores.formatar(parcelaPreview)} · Total ${Valores.formatar(totalPreview)}"
            estado.valorCentavos > 0 -> "Pronto para salvar"
            else -> "Digite no teclado abaixo"
        }
    }

    fun parcelamento(estado: NovoLancamentoUiState): String = formatarParcelamentoCampo(estado.totalParcelas, estado.modoParcelamento)

    fun mensagemDasParcelas(estado: NovoLancamentoUiState): String? = when {
        CampoDeLancamentoManual.PARCELAS in estado.erros ->
            "Parcelamento disponível apenas em despesa no crédito"
        estado.parcelado && estado.valorCentavos > 0L ->
            resumoParcelas(estado)
        else -> null
    }

    fun resumoParcelas(estado: NovoLancamentoUiState): String? =
        formatarResumoParcelas(estado.totalParcelas, estado.valorParcelaPreviewCentavos, estado.valorTotalPreviewCentavos)

    fun formatarParcelamentoCampo(totalParcelas: Int, modoParcelamento: ModoParcelamento): String = when {
        totalParcelas <= 1 -> "À vista (1x)"
        modoParcelamento == ModoParcelamento.VALOR_PARCELA -> "${totalParcelas}x (valor de cada parcela)"
        else -> "${totalParcelas}x (valor total)"
    }

    fun formatarResumoParcelas(totalParcelas: Int, parcelaCentavos: Long?, totalCentavos: Long?): String? {
        if (parcelaCentavos == null || totalCentavos == null) return null
        return "$totalParcelas parcelas de ${Valores.formatar(parcelaCentavos)} · Total ${Valores.formatar(totalCentavos)}"
    }

    fun origemIgualAoDestino(estado: NovoLancamentoUiState): Boolean =
        estado.tipo == TipoDoLancamentoManual.TRANSFERENCIA && estado.instrumento != null && estado.instrumento == estado.destino

    fun mensagemDoInstrumento(estado: NovoLancamentoUiState): String? = when {
        estado.semInstrumentos -> "Nenhum instrumento cadastrado. Cadastre um banco para lançar."
        CampoDeLancamentoManual.INSTRUMENTO !in estado.erros -> null
        estado.tipo == TipoDoLancamentoManual.RECEITA -> "Informe onde o dinheiro entrou"
        else -> "Informe de onde saiu o dinheiro"
    }

    fun mensagemDaOrigem(estado: NovoLancamentoUiState): String? =
        if (CampoDeLancamentoManual.ORIGEM in estado.erros) "Escolha a conta de origem" else null

    fun mensagemDoDestino(estado: NovoLancamentoUiState): String? = when {
        CampoDeLancamentoManual.DESTINO !in estado.erros -> null
        estado.instrumento != null && estado.instrumento == estado.destino -> "Origem e destino precisam ser diferentes"
        else -> "Escolha a conta de destino"
    }

    private fun capitalizado(texto: String): String = texto.replaceFirstChar { letra -> letra.titlecase(LOCALE) }
}

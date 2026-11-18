package com.pingo.feature.transactions.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.data.manual.CampoDeLancamentoManual
import com.pingo.core.model.fatura.ModoParcelamento
import java.time.LocalDate

enum class TipoDoLancamentoManual(val rotulo: String) {
    DESPESA("Despesa"),
    RECEITA("Receita"),
    TRANSFERENCIA("Transferência"),
}

enum class PainelDoNovoLancamento {
    INSTRUMENTO,
    DESTINO,
    CATEGORIA,
    PARCELAS,
}

@Immutable
data class OpcaoDeInstrumento(val publicId: String, val rotulo: String, val ehCredito: Boolean = false)

@Immutable
data class NovoLancamentoUiState(
    val carregando: Boolean = true,
    val tipo: TipoDoLancamentoManual = TipoDoLancamentoManual.DESPESA,
    val valorCentavos: Long = 0,
    val instrumento: OpcaoDeInstrumento? = null,
    val destino: OpcaoDeInstrumento? = null,
    val categoria: OpcaoDeCategoria? = null,
    val categoriaSugerida: Boolean = false,
    val data: LocalDate = LocalDate.MIN,
    val hoje: LocalDate = LocalDate.MIN,
    val estabelecimento: String = "",
    val totalParcelas: Int = 1,
    val modoParcelamento: ModoParcelamento = ModoParcelamento.VALOR_TOTAL,
    val instrumentos: List<OpcaoDeInstrumento> = emptyList(),
    val categorias: List<OpcaoDeCategoria> = emptyList(),
    val painel: PainelDoNovoLancamento? = null,
    val calendarioVisivel: Boolean = false,
    val erros: Set<CampoDeLancamentoManual> = emptySet(),
    val salvando: Boolean = false,
    val salvo: Boolean = false,
) {
    val semInstrumentos: Boolean
        get() = !carregando && instrumentos.isEmpty()

    val podeParcelar: Boolean
        get() = tipo == TipoDoLancamentoManual.DESPESA && instrumento?.ehCredito == true

    val parcelado: Boolean
        get() = podeParcelar && totalParcelas > 1

    val valorTotalPreviewCentavos: Long?
        get() = if (!parcelado || valorCentavos <= 0L) {
            null
        } else {
            when (modoParcelamento) {
                ModoParcelamento.VALOR_TOTAL -> valorCentavos
                ModoParcelamento.VALOR_PARCELA -> valorCentavos * totalParcelas
            }
        }

    val valorParcelaPreviewCentavos: Long?
        get() = if (!parcelado || valorCentavos <= 0L) {
            null
        } else {
            when (modoParcelamento) {
                ModoParcelamento.VALOR_TOTAL -> valorCentavos.takeIf { it >= totalParcelas }?.div(totalParcelas)
                ModoParcelamento.VALOR_PARCELA -> valorCentavos
            }
        }

    val podeSalvar: Boolean
        get() = !carregando && !salvando && !salvo
}

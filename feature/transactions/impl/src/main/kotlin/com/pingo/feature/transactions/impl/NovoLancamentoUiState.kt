package com.pingo.feature.transactions.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.data.manual.CampoDeLancamentoManual
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
}

@Immutable
data class OpcaoDeInstrumento(val publicId: String, val rotulo: String)

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

    val podeSalvar: Boolean
        get() = !carregando && !salvando && !salvo
}

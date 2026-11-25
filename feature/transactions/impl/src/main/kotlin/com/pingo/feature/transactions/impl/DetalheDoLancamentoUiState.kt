package com.pingo.feature.transactions.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.data.detalhe.CampoDoDetalhe
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.fatura.ModoParcelamento
import java.time.Instant
import java.time.LocalDate

enum class PainelDoDetalhe {
    INSTRUMENTO,
    DESTINO,
    CATEGORIA,
    PARCELAS,
    MARCAR_TRANSFERENCIA,
}

enum class MensagemDoDetalhe {
    INSTRUMENTO_PARA_TRANSFERENCIA,
    DESTINO_OBRIGATORIO,
    MESMA_CONTA,
    PAR_AMBIGUO,
    PAR_NAO_RESOLVIDO,
    DIRECAO_DESCONHECIDA,
    PAR_INCONSISTENTE,
    FALHA_AO_SALVAR,
}

@Immutable
sealed interface OrigemDoLancamentoUi {
    @Immutable
    data class Manual(val registradoEm: Instant) : OrigemDoLancamentoUi

    @Immutable
    data class Capturado(val instituicao: InstituicaoFinanceira?, val lidoEm: Instant, val textoBruto: String?, val confianca: Double) :
        OrigemDoLancamentoUi
}

@Immutable
data class ParDaTransferenciaUi(
    val publicId: String,
    val rotuloDoVinculo: String,
    val entrada: Boolean?,
    val valorCentavos: Long,
    val dataHora: Instant,
)

@Immutable
data class ParcelamentoDetalheUi(val parcelaNumero: Int, val totalParcelas: Int, val valorTotalCompraCentavos: Long?)

@Immutable
internal data class ConfiguracaoPainelDetalhe(val selecionado: String?, val titulo: String, val prefixo: String)

@Immutable
data class DetalheDoLancamentoUiState(
    val carregando: Boolean = true,
    val naoEncontrado: Boolean = false,
    val ehTransferencia: Boolean = false,
    val tipo: TipoDoLancamentoManual = TipoDoLancamentoManual.DESPESA,
    val valorCentavos: Long = 0,
    val instrumento: OpcaoDeInstrumento? = null,
    val destino: OpcaoDeInstrumento? = null,
    val categoria: OpcaoDeCategoria? = null,
    val dataHora: Instant = Instant.EPOCH,
    val hoje: LocalDate = LocalDate.MIN,
    val estabelecimento: String = "",
    val aplicarCategoriaATodos: Boolean = false,
    val totalParcelas: Int = 1,
    val modoParcelamento: ModoParcelamento = ModoParcelamento.VALOR_TOTAL,
    val parcelamento: ParcelamentoDetalheUi? = null,
    val podeEditarParcelamento: Boolean = true,
    val parcelamentoAlterado: Boolean = false,
    val instrumentos: List<OpcaoDeInstrumento> = emptyList(),
    val destinosDaTransferencia: List<OpcaoDeInstrumento> = emptyList(),
    val categorias: List<OpcaoDeCategoria> = emptyList(),
    val origem: OrigemDoLancamentoUi? = null,
    val par: ParDaTransferenciaUi? = null,
    val painel: PainelDoDetalhe? = null,
    val calendarioVisivel: Boolean = false,
    val tecladoVisivel: Boolean = false,
    val erros: Set<CampoDoDetalhe> = emptySet(),
    val salvando: Boolean = false,
    val salvo: Boolean = false,
    val dialogoDeExclusao: Boolean = false,
    val dialogoDeExclusaoParcelas: Boolean = false,
    val excluindo: Boolean = false,
    val excluido: Boolean = false,
    val ocupado: Boolean = false,
    val mensagem: MensagemDoDetalhe? = null,
) {
    val semInstrumentos: Boolean
        get() = !carregando && instrumentos.isEmpty()

    val podeParcelar: Boolean
        get() = !carregando &&
            !ehTransferencia &&
            podeEditarParcelamento &&
            tipo == TipoDoLancamentoManual.DESPESA &&
            instrumento?.ehCredito == true

    val exibirCampoParcelamento: Boolean
        get() = !carregando &&
            !ehTransferencia &&
            podeEditarParcelamento &&
            tipo == TipoDoLancamentoManual.DESPESA &&
            (instrumento == null || instrumento.ehCredito)

    val parcelamentoConfigurado: Boolean
        get() = podeParcelar && totalParcelas > 1

    val exibirPreviewParcelamento: Boolean
        get() = parcelamentoConfigurado && valorCentavos > 0L && (parcelamento == null || parcelamentoAlterado)

    val valorTotalPreviewCentavos: Long?
        get() = if (!exibirPreviewParcelamento) {
            null
        } else {
            when (modoParcelamento) {
                ModoParcelamento.VALOR_TOTAL -> valorCentavos
                ModoParcelamento.VALOR_PARCELA -> valorCentavos * totalParcelas
            }
        }

    val valorParcelaPreviewCentavos: Long?
        get() = if (!exibirPreviewParcelamento) {
            null
        } else {
            when (modoParcelamento) {
                ModoParcelamento.VALOR_TOTAL -> valorCentavos.takeIf { it >= totalParcelas }?.div(totalParcelas)
                ModoParcelamento.VALOR_PARCELA -> valorCentavos
            }
        }

    val podeSalvar: Boolean
        get() = !carregando && !naoEncontrado && !salvando && !excluindo && !ocupado && !salvo && !excluido

    val podeAlterarTipo: Boolean
        get() = !carregando && !ehTransferencia && !ocupado

    val podeAlternarValor: Boolean
        get() = !carregando && !naoEncontrado && !salvando && !excluindo && !ocupado
}

package com.pingo.feature.home.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.organizacao.EstadoBlocoFinanceiro
import com.pingo.core.model.organizacao.FaturaAbertaInicio
import com.pingo.core.model.organizacao.SaldoVoucherInicio
import com.pingo.core.network.update.AtualizacaoDisponivel
import com.pingo.core.network.update.EstadoAtualizacaoDoApp
import com.pingo.core.notifications.SituacaoDaCaptura
import java.io.File
import java.time.YearMonth

@Immutable
data class AcoesAtualizacaoHome(
    val onAbrir: () -> Unit = {},
    val onFechar: () -> Unit = {},
    val onVerificar: () -> Unit = {},
    val onBaixar: (AtualizacaoDisponivel) -> Unit = {},
    val onInstalar: (File) -> Unit = {},
    val onPermitirFontes: () -> Unit = {},
    val onAbrirRelease: (String) -> Unit = {},
)

@Immutable
data class LancamentoDaInicio(val lancamento: Lancamento, val categoria: Categoria?, val instrumento: Instrumento?, val conta: Conta?)

@Immutable
data class CategoriaDoTopo(val publicId: String, val nome: String, val valorCentavos: Long, val percentual: Int)

@Immutable
data class HomeUiState(
    val carregando: Boolean = true,
    val saudacao: String = "Olá,",
    val capturaAutomatica: Boolean = true,
    val situacao: SituacaoDaCaptura = SituacaoDaCaptura.ATIVA,
    val motivo: MotivoParada? = null,
    val semLeituraHa48h: Boolean = false,
    val mes: YearMonth? = null,
    val podeVoltarMes: Boolean = false,
    val podeAvancarMes: Boolean = false,
    val saldoCentavos: Long = 0,
    val receitasCentavos: Long = 0,
    val despesasCentavos: Long = 0,
    val pendentes: Int = 0,
    val topCategorias: List<CategoriaDoTopo> = emptyList(),
    val ultimos: List<LancamentoDaInicio> = emptyList(),
    val faturasAbertas: EstadoBlocoFinanceiro<List<FaturaAbertaInicio>> = EstadoBlocoFinanceiro.Sucesso(emptyList()),
    val saldosVoucher: EstadoBlocoFinanceiro<List<SaldoVoucherInicio>> = EstadoBlocoFinanceiro.Sucesso(emptyList()),
    val vazia: Boolean = false,
    val versaoApp: String = "",
    val estadoAtualizacao: EstadoAtualizacaoDoApp = EstadoAtualizacaoDoApp.Ocioso,
    val mostrarSheetAtualizacao: Boolean = false,
) {
    val capturaParada: Boolean
        get() = !capturaAutomatica || situacao != SituacaoDaCaptura.ATIVA || semLeituraHa48h

    val temAtualizacaoDisponivel: Boolean
        get() = estadoAtualizacao is EstadoAtualizacaoDoApp.Disponivel ||
            estadoAtualizacao is EstadoAtualizacaoDoApp.Baixando ||
            estadoAtualizacao is EstadoAtualizacaoDoApp.ProntoParaInstalar ||
            (estadoAtualizacao as? EstadoAtualizacaoDoApp.Falha)?.info != null
}

package com.pingo.feature.review.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.data.detalhe.MotivoDaRecusa
import com.pingo.core.data.detalhe.ResultadoDaEdicao
import com.pingo.core.data.detalhe.ResultadoDaTransferencia
import com.pingo.core.data.revisao.ResultadoDaConfirmacao
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.TipoLancamento
import java.time.Instant

enum class MotivoDaRevisao {
    BAIXA_CONFIANCA,
    POSSIVEL_DUPLICATA,
    TRANSFERENCIA_AMBIGUA,
    ESTABELECIMENTO_NOVO,
    INSTRUMENTO_AMBIGUO,
    INSTRUMENTO_NAO_ENCONTRADO,
    MOEDA_ESTRANGEIRA,
    DADOS_INCOMPLETOS,
    OUTRO,
}

@Immutable
data class OpcaoDaRevisao(val publicId: String, val nome: String)

@Immutable
data class PendenciaDaRevisao(
    val publicId: String,
    val motivo: MotivoDaRevisao,
    val confianca: Double,
    val estabelecimento: String?,
    val valorCentavos: Long,
    val tipo: TipoLancamento,
    val dataHora: Instant,
    val horario: String,
    val vinculo: String,
    val categoriaNome: String?,
    val textoInterpretado: String?,
    val podeConfirmar: Boolean,
)

@Immutable
data class TrocaDeCategoriaDaRevisao(
    val lancamentoPublicId: String,
    val estabelecimento: String,
    val valorCentavos: Long,
    val opcoes: List<OpcaoDaRevisao>,
    val selecionadaPublicId: String? = null,
    val aplicarATodos: Boolean = false,
) {
    val selecionada: OpcaoDaRevisao?
        get() = opcoes.firstOrNull { it.publicId == selecionadaPublicId }

    val podeAplicar: Boolean
        get() = selecionada != null
}

@Immutable
data class MarcarTransferenciaDaRevisao(
    val lancamentoPublicId: String,
    val estabelecimento: String,
    val valorCentavos: Long,
    val opcoes: List<OpcaoDaRevisao>,
)

@Immutable
sealed interface MensagemDaRevisao {
    data object InstrumentoNecessario : MensagemDaRevisao

    data class LoteComPendentes(val restantes: Int) : MensagemDaRevisao

    data object TransferenciaBloqueada : MensagemDaRevisao

    data object MesmaConta : MensagemDaRevisao

    data object ParAmbigua : MensagemDaRevisao

    data object FalhaAoSalvar : MensagemDaRevisao
}

@Immutable
data class ReviewUiState(
    val carregando: Boolean = true,
    val pendentes: List<PendenciaDaRevisao> = emptyList(),
    val painelDeCategoria: TrocaDeCategoriaDaRevisao? = null,
    val painelDeTransferencia: MarcarTransferenciaDaRevisao? = null,
    val ocupado: Boolean = false,
    val mensagem: MensagemDaRevisao? = null,
) {
    val quantidade: Int
        get() = pendentes.size

    val vazia: Boolean
        get() = !carregando && pendentes.isEmpty()

    val podeConfirmarTodos: Boolean
        get() = pendentes.isNotEmpty() && !ocupado
}

internal fun motivoDe(motivo: String?): MotivoDaRevisao = when (motivo) {
    MotivoPendencia.BAIXA_CONFIANCA.name -> MotivoDaRevisao.BAIXA_CONFIANCA
    MotivoPendencia.POSSIVEL_DUPLICATA.name -> MotivoDaRevisao.POSSIVEL_DUPLICATA
    MotivoPendencia.TRANSFERENCIA_AMBIGUA.name -> MotivoDaRevisao.TRANSFERENCIA_AMBIGUA
    MotivoPendencia.CATEGORIA_DESCONHECIDA.name -> MotivoDaRevisao.ESTABELECIMENTO_NOVO
    MotivoPendencia.INSTRUMENTO_AMBIGUO.name -> MotivoDaRevisao.INSTRUMENTO_AMBIGUO
    MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name -> MotivoDaRevisao.INSTRUMENTO_NAO_ENCONTRADO
    MotivoPendencia.MOEDA_ESTRANGEIRA.name -> MotivoDaRevisao.MOEDA_ESTRANGEIRA
    MotivoPendencia.CAMPOS_INCOMPLETOS.name -> MotivoDaRevisao.DADOS_INCOMPLETOS
    else -> MotivoDaRevisao.OUTRO
}

internal fun mensagemDe(confirmacao: ResultadoDaConfirmacao): MensagemDaRevisao? = when {
    confirmacao.mantidos == 0 -> null
    confirmacao.confirmados == 0 -> MensagemDaRevisao.InstrumentoNecessario
    else -> MensagemDaRevisao.LoteComPendentes(confirmacao.mantidos)
}

internal fun mensagemDe(edicao: ResultadoDaEdicao): MensagemDaRevisao? = when (edicao) {
    is ResultadoDaEdicao.Invalido -> MensagemDaRevisao.FalhaAoSalvar
    ResultadoDaEdicao.NaoEncontrado -> MensagemDaRevisao.FalhaAoSalvar
    is ResultadoDaEdicao.Salvo -> null
}

internal fun mensagemDe(transferencia: ResultadoDaTransferencia): MensagemDaRevisao? = when (transferencia) {
    is ResultadoDaTransferencia.Marcada -> null
    is ResultadoDaTransferencia.Desfeita -> null
    ResultadoDaTransferencia.NaoEncontrado -> MensagemDaRevisao.FalhaAoSalvar
    is ResultadoDaTransferencia.Recusada -> when (transferencia.motivo) {
        MotivoDaRecusa.MESMA_CONTA -> MensagemDaRevisao.MesmaConta
        MotivoDaRecusa.PAR_AMBIGUO, MotivoDaRecusa.PAR_NAO_RESOLVIDO -> MensagemDaRevisao.ParAmbigua
        else -> MensagemDaRevisao.TransferenciaBloqueada
    }
}

package com.pingo.core.model.fatura

import androidx.compose.runtime.Immutable
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusFatura
import java.time.LocalDate

enum class TipoCicloFatura {
    ABERTA,
    PROXIMA,
    FECHADA,
}

@Immutable
data class CicloDeFaturaDetalhado(
    val faturaPublicId: String?,
    val instrumentoPublicId: String,
    val competencia: LocalDate,
    val dataFechamento: LocalDate,
    val dataVencimento: LocalDate,
    val totalCentavos: Long,
    val status: StatusFatura,
    val tipoCiclo: TipoCicloFatura,
    val diasParaVencimento: Long,
    val vencida: Boolean,
    val lancamentos: List<Lancamento>,
)

sealed interface EstadoFaturaCartao {
    @Immutable
    data class Configurada(
        val faturaPublicId: String?,
        val competencia: LocalDate,
        val dataFechamento: LocalDate,
        val dataVencimento: LocalDate,
        val totalCentavos: Long,
        val status: StatusFatura,
        val diasParaVencimento: Long,
        val vencida: Boolean,
    ) : EstadoFaturaCartao

    @Immutable
    data object ConfiguracaoIncompleta : EstadoFaturaCartao
}

@Immutable
data class FaturasDoCartao(
    val instrumento: Instrumento,
    val conta: Conta?,
    val configuracaoIncompleta: Boolean,
    val ciclos: List<CicloDeFaturaDetalhado>,
)

package com.pingo.feature.accounts.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.model.diaDoMesInformado
import java.time.LocalDate

@Immutable
data class ModalCompletarDatasUiState(
    val instrumentoPublicId: String,
    val tituloCartao: String,
    val fechamentoDia: String = "",
    val vencimentoDia: String = "",
    val salvando: Boolean = false,
    val erro: String? = null,
) {
    val fechamentoInt: Int?
        get() = fechamentoDia.trim().toIntOrNull()

    val vencimentoInt: Int?
        get() = vencimentoDia.trim().toIntOrNull()

    val fechamentoValido: Boolean
        get() = diaDoMesInformado(fechamentoInt)

    val vencimentoValido: Boolean
        get() = diaDoMesInformado(vencimentoInt)

    val podeSalvar: Boolean
        get() = !salvando && fechamentoValido && vencimentoValido
}

@Immutable
data class AccountsUiState(
    val carregando: Boolean = true,
    val falha: Boolean = false,
    val grupos: List<GrupoDeContas> = emptyList(),
    val arquivadas: List<ContaArquivadaDaLista> = emptyList(),
    val hoje: LocalDate = LocalDate.now(),
    val modalCompletarDatas: ModalCompletarDatasUiState? = null,
    val modalAjustarSaldo: ModalAjustarSaldoUiState? = null,
) {
    val vazia: Boolean
        get() = !carregando && !falha && grupos.isEmpty() && arquivadas.isEmpty()
}

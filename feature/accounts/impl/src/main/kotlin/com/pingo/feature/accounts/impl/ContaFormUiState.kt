package com.pingo.feature.accounts.impl

import androidx.compose.runtime.Immutable
import com.pingo.core.ui.contas.ContaFormulario

@Immutable
data class ContaFormUiState(
    val carregando: Boolean = false,
    val falhaDeLeitura: Boolean = false,
    val formulario: ContaFormulario = ContaFormulario.nova(),
    val salvando: Boolean = false,
    val falhouAoSalvar: Boolean = false,
    val concluido: Boolean = false,
    val edicao: Boolean = false,
) {
    val podeSalvar: Boolean
        get() = !carregando && !salvando && !concluido && !falhaDeLeitura && formulario.valida()
}

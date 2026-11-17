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
    val contaArquivada: Boolean = false,
    val dialogo: DialogoDeArquivamento? = null,
    val processandoArquivamento: Boolean = false,
    val falhouAoArquivar: Boolean = false,
) {
    val formularioEditavel: Boolean
        get() = !salvando && !processandoArquivamento

    val podeSalvar: Boolean
        get() = !carregando && formularioEditavel && !concluido && !falhaDeLeitura && !contaArquivada && formulario.valida()
}

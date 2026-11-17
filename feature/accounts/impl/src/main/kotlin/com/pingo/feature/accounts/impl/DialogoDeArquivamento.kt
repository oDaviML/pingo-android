package com.pingo.feature.accounts.impl

import androidx.compose.runtime.Immutable

@Immutable
sealed interface DialogoDeArquivamento {
    data object Conta : DialogoDeArquivamento

    data class Instrumento(val chave: String, val apelido: String) : DialogoDeArquivamento
}

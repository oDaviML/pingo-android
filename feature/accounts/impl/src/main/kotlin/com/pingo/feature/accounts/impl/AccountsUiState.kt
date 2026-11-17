package com.pingo.feature.accounts.impl

import androidx.compose.runtime.Immutable

@Immutable
data class AccountsUiState(val carregando: Boolean = true, val falha: Boolean = false, val grupos: List<GrupoDeContas> = emptyList()) {
    val vazia: Boolean
        get() = !carregando && !falha && grupos.isEmpty()
}

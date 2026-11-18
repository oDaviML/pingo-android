package com.pingo.feature.settings.impl

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsUiState(
    val avisoAssinaturasAtivo: Boolean = false,
    val permissaoNotificacaoNegada: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val deleting: Boolean = false,
    val deleteFailed: Boolean = false,
    val accountDeleted: Boolean = false,
)

@Immutable
data class SettingsNotificacoesUi(
    val avisoAssinaturasAtivo: Boolean = false,
    val permissaoNotificacaoNegada: Boolean = false,
    val onToggleAvisoAssinaturas: (Boolean) -> Unit = {},
    val onAbrirConfiguracoesNotificacao: () -> Unit = {},
)

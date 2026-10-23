package com.pingo.feature.settings.impl

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsUiState(
    val showDeleteDialog: Boolean = false,
    val deleting: Boolean = false,
    val deleteFailed: Boolean = false,
    val accountDeleted: Boolean = false,
)

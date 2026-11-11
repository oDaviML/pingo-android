package com.pingo.feature.transactions.impl

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
internal data class CampoSelecionavel(
    val rotulo: String,
    val texto: String,
    val preenchido: Boolean,
    val icone: ImageVector,
    val testTag: String,
    val erro: Boolean = false,
    val mensagem: String? = null,
)

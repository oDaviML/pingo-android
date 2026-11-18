package com.pingo.feature.accounts.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FaturasScreen(onVoltar: () -> Unit, onAbrirLancamento: (String) -> Unit = {}, viewModel: FaturasViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FaturasContent(
        state = state,
        onVoltar = onVoltar,
        onAbrirLancamento = onAbrirLancamento,
        onEvent = viewModel::onEvent,
    )
}

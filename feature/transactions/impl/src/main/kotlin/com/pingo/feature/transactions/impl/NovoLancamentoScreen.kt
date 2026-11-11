package com.pingo.feature.transactions.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NovoLancamentoScreen(onVoltar: () -> Unit, viewModel: NovoLancamentoViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.salvo) {
        if (state.salvo) {
            onVoltar()
        }
    }

    NovoLancamentoContent(
        state = state,
        onEvent = viewModel::onEvent,
        onFechar = onVoltar,
    )
}

package com.pingo.feature.accounts.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ContaFormScreen(onVoltar: () -> Unit, viewModel: ContaFormViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.concluido) {
        if (state.concluido) {
            onVoltar()
        }
    }

    ContaFormContent(
        state = state,
        onEvent = viewModel::onEvent,
        onVoltar = onVoltar,
    )
}

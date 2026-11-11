package com.pingo.feature.transactions.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DetalheDoLancamentoScreen(publicId: String, onBack: () -> Unit, viewModel: DetalheDoLancamentoViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.salvo, state.excluido) {
        if (state.salvo || state.excluido) {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("detalhe-$publicId")) {
        DetalheDoLancamentoContent(
            state = state,
            onEvent = viewModel::onEvent,
            onVoltar = onBack,
        )
    }
}

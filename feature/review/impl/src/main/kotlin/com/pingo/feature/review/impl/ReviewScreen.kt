package com.pingo.feature.review.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ReviewScreen(onCorrigir: (String) -> Unit, onVoltar: () -> Unit, viewModel: ReviewViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ReviewContent(
        state = state,
        onEvent = viewModel::onEvent,
        onCorrigir = onCorrigir,
        onVoltar = onVoltar,
    )
}

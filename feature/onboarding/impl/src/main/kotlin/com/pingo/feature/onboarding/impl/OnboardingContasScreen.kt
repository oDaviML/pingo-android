package com.pingo.feature.onboarding.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingContasScreen(onConcluir: () -> Unit, viewModel: OnboardingContasViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.concluido) {
        if (state.concluido) {
            onConcluir()
        }
    }

    OnboardingContasContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

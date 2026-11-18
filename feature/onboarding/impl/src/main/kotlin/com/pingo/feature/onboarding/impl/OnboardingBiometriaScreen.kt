package com.pingo.feature.onboarding.impl

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingBiometriaScreen(onConcluir: () -> Unit, viewModel: OnboardingBiometriaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.concluido) {
        if (state.concluido) {
            onConcluir()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.atualizarDisponibilidade()
    }

    OnboardingBiometriaContent(
        state = state,
        onAtivar = { viewModel.ativar(context.encontrarActivity()) },
        onAgoraNao = viewModel::pularOuAgoraNao,
        onAbrirConfiguracoesDeSeguranca = {
            context.encontrarActivity()?.let(viewModel::abrirConfiguracoesDeSeguranca)
        },
    )
}

private tailrec fun Context.encontrarActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.encontrarActivity()
    else -> null
}

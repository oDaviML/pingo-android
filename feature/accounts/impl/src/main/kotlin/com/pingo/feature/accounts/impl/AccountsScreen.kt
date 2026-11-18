package com.pingo.feature.accounts.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingo.core.designsystem.component.AbaInferior

@Suppress("LongParameterList")
@Composable
fun AccountsScreen(
    onSelecionarAba: (AbaInferior) -> Unit,
    onNovaConta: () -> Unit,
    onAbrirConta: (String) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
    onOpenNovo: (() -> Unit)? = null,
    mostrarBarraInferior: Boolean = true,
    onAbrirFaturas: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AccountsContent(
        state = state,
        onNovaConta = onNovaConta,
        onAbrirConta = onAbrirConta,
        onTentarDeNovo = viewModel::tentarDeNovo,
        onSelecionarAba = onSelecionarAba,
        onOpenNovo = onOpenNovo,
        mostrarBarraInferior = mostrarBarraInferior,
        onAbrirFaturas = onAbrirFaturas,
        onEvent = viewModel::onEvent,
    )
}

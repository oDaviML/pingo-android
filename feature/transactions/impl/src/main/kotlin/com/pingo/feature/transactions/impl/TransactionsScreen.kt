package com.pingo.feature.transactions.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingo.core.designsystem.component.AbaInferior

@Composable
fun TransactionsScreen(
    onOpenDetalhe: (String) -> Unit,
    onOpenNovo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TransactionsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenDetalhe = onOpenDetalhe,
        onOpenNovo = onOpenNovo,
        onSelecionarAba = onSelecionarAba,
    )
}

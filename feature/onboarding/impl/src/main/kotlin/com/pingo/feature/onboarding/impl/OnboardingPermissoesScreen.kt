package com.pingo.feature.onboarding.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingPermissoesScreen(
    onConcluir: () -> Unit,
    viewModel: OnboardingPermissoesViewModel = hiltViewModel(),
    permissaoDeNotificacao: PermissaoDeNotificacao = rememberPermissaoDeNotificacao(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.concluido) {
        if (state.concluido) {
            onConcluir()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.atualizarEstadosDoSistema()
    }

    OnboardingPermissoesContent(
        state = state,
        onEvent = { event ->
            viewModel.onEvent(event)

            when (event) {
                OnboardingPermissoesEvent.AbrirAjustesDeNotificacoes ->
                    IntentsDeSistema.abrir(context, IntentsDeSistema.acessoAsNotificacoes())

                OnboardingPermissoesEvent.PedirNotificacoes ->
                    permissaoDeNotificacao.solicitar(viewModel::registrarNotificacoesDoSistema)

                OnboardingPermissoesEvent.CorrigirNotificacoes ->
                    IntentsDeSistema.abrir(context, IntentsDeSistema.notificacoesDoApp(context.packageName))

                OnboardingPermissoesEvent.AbrirAjustesDeBateria ->
                    IntentsDeSistema.abrir(context, IntentsDeSistema.ajustesDeBateria())

                OnboardingPermissoesEvent.AbrirInicioAutomatico ->
                    IntentsDeSistema.abrir(
                        context,
                        IntentsDeSistema.inicioAutomatico(state.fabricanteSelecionado)
                            ?: IntentsDeSistema.detalhesDoApp(context.packageName),
                    )

                else -> Unit
            }
        },
    )
}

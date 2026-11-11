package com.pingo.feature.home.impl

import androidx.lifecycle.ViewModel
import com.pingo.core.notifications.AcessoNotificacoes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val acessoNotificacoes: AcessoNotificacoes) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState(capturaAutomatica = acessoNotificacoes.concedido()))

    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    fun atualizarCaptura() {
        mutableUiState.value = HomeUiState(capturaAutomatica = acessoNotificacoes.concedido())
    }
}

package com.pingo.feature.accounts.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val contaRepository: ContaRepository,
    private val instrumentoRepository: InstrumentoRepository,
) : ViewModel() {
    private val tentativa = MutableStateFlow(0)
    private val mutableUiState = MutableStateFlow(AccountsUiState())

    val uiState: StateFlow<AccountsUiState> = mutableUiState.asStateFlow()

    init {
        observar()
    }

    fun tentarDeNovo() {
        tentativa.update { it + 1 }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observar() {
        viewModelScope.launch {
            tentativa
                .flatMapLatest {
                    mutableUiState.update { it.copy(carregando = true, falha = false) }
                    combine(
                        contaRepository.observeContas(),
                        instrumentoRepository.observeInstrumentos(),
                    ) { contas, instrumentos -> agruparContas(contas, instrumentos) }
                        .catch {
                            mutableUiState.update { it.copy(carregando = false, falha = true) }
                        }
                }
                .collect { grupos ->
                    mutableUiState.update { it.copy(carregando = false, falha = false, grupos = grupos) }
                }
        }
    }
}

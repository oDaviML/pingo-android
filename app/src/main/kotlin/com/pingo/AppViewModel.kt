package com.pingo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.SessionState
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.navigation.DestinoInicial
import com.pingo.navigation.destinoInicialPara
import com.pingo.sync.SyncScheduler
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferences: UserPreferencesDataSource,
    private val syncScheduler: Lazy<SyncScheduler>,
) : ViewModel() {
    val session: StateFlow<SessionState> = authRepository.session

    val destinoInicial: StateFlow<DestinoInicial?> = authRepository.session
        .filter { it != SessionState.Loading }
        .take(1)
        .map { session ->
            destinoInicialPara(
                sessao = session,
                consentimentoRegistrado = preferences.consentVersion() != null,
                contasConfiguradas = preferences.contasConfiguradas(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun onConsentAccepted() {
        syncScheduler.get().syncNow()
    }
}

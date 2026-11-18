package com.pingo.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.AccountRepository
import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val preferences: UserPreferencesDataSource,
    private val agendadorDeAvisos: AgendadorDeAvisosOpcional,
) : ViewModel() {
    constructor(accountRepository: AccountRepository) : this(
        accountRepository = accountRepository,
        preferences = SemPreferencias,
        agendadorDeAvisos = AgendadorDeAvisosOpcional(),
    )

    private val mutableUiState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        if (preferences !== SemPreferencias) {
            viewModelScope.launch {
                preferences.avisoAssinaturasAtivo.collect { ativo ->
                    mutableUiState.update { it.copy(avisoAssinaturasAtivo = ativo) }
                }
            }
        }
    }

    fun alternarAvisoAssinaturas(ativo: Boolean) {
        viewModelScope.launch {
            preferences.setAvisoAssinaturasAtivo(ativo)
            mutableUiState.update {
                it.copy(
                    avisoAssinaturasAtivo = ativo,
                    permissaoNotificacaoNegada = false,
                )
            }
            if (ativo) {
                agendadorDeAvisos.reconciliarTodas()
            } else {
                agendadorDeAvisos.cancelarTodos()
            }
        }
    }

    fun aoNegarPermissaoDeNotificacao() {
        viewModelScope.launch {
            preferences.setAvisoAssinaturasAtivo(false)
            mutableUiState.update {
                it.copy(
                    avisoAssinaturasAtivo = false,
                    permissaoNotificacaoNegada = true,
                )
            }
            agendadorDeAvisos.cancelarTodos()
        }
    }

    fun sincronizarPermissaoDeNotificacao(concedida: Boolean) {
        if (concedida) {
            mutableUiState.update { it.copy(permissaoNotificacaoNegada = false) }
        } else if (mutableUiState.value.avisoAssinaturasAtivo) {
            aoNegarPermissaoDeNotificacao()
        }
    }

    fun showDeleteDialog() {
        mutableUiState.update { it.copy(showDeleteDialog = true, deleteFailed = false) }
    }

    fun dismissDeleteDialog() {
        if (mutableUiState.value.deleting) {
            return
        }

        mutableUiState.update { it.copy(showDeleteDialog = false, deleteFailed = false) }
    }

    fun deleteAccount() {
        if (mutableUiState.value.deleting) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { it.copy(deleting = true, deleteFailed = false) }

            runCatching { accountRepository.deleteAccount() }
                .fold(
                    onSuccess = {
                        mutableUiState.update {
                            it.copy(deleting = false, showDeleteDialog = false, accountDeleted = true)
                        }
                    },
                    onFailure = { exception ->
                        if (exception is CancellationException) {
                            throw exception
                        }

                        mutableUiState.update { it.copy(deleting = false, deleteFailed = true) }
                    },
                )
        }
    }

    private object SemPreferencias : UserPreferencesDataSource {
        override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = ""

        override suspend fun activatedDeviceId(): String? = null

        override suspend fun setActivatedDeviceId(deviceId: String) = Unit

        override suspend fun consentVersion(): String? = null

        override suspend fun consentAt(): Instant? = null

        override suspend fun setConsent(version: String, instant: Instant) = Unit

        override suspend fun contasConfiguradas(): Boolean = false

        override suspend fun setContasConfiguradas(configuradas: Boolean) = Unit

        override suspend fun contasOnboardingPendentes(): List<String> = emptyList()

        override suspend fun setContasOnboardingPendentes(publicIds: List<String>) = Unit

        override suspend fun clear() = Unit
    }
}

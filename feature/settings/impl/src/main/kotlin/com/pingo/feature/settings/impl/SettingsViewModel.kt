package com.pingo.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val accountRepository: AccountRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

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
}

package com.pingo

import androidx.lifecycle.ViewModel
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(authRepository: AuthRepository) : ViewModel() {
    val session: StateFlow<SessionState> = authRepository.session
}

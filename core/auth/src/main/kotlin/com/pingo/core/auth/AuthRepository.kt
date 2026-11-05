package com.pingo.core.auth

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val session: StateFlow<SessionState>

    suspend fun loginWithGoogle(activity: Activity): LoginResult

    suspend fun logout()

    suspend fun accessToken(forceRefresh: Boolean = false): String?
}

enum class SessionState {
    Loading,
    LoggedOut,
    LoggedIn,
}

sealed interface LoginResult {
    data object Success : LoginResult

    data object Cancelled : LoginResult

    data object Failure : LoginResult
}

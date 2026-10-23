package com.pingo.core.auth

import android.app.Activity
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.pingo.core.common.AppDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Auth0Repository @Inject constructor(
    private val config: Auth0Config,
    private val tokenStore: TokenStore,
    private val dispatchers: AppDispatchers,
) : AuthRepository {
    private val account = Auth0.getInstance(config.clientId, config.domain)
    private val authentication = AuthenticationAPIClient(account)
    private val refreshMutex = Mutex()
    private val sessionScope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val mutableSession = MutableStateFlow(SessionState.Loading)

    override val session: StateFlow<SessionState> = mutableSession.asStateFlow()

    init {
        sessionScope.launch {
            mutableSession.value = if (tokenStore.read() == null) {
                SessionState.LoggedOut
            } else {
                SessionState.LoggedIn
            }
        }
    }

    override suspend fun loginWithGoogle(activity: Activity): LoginResult = withContext(dispatchers.io) {
        try {
            val credentials = WebAuthProvider.login(account)
                .withScheme(config.scheme)
                .withConnection(GOOGLE_CONNECTION)
                .withScope(SCOPE)
                .withAudience(config.audience)
                .await(activity)

            tokenStore.write(credentials.toStoredTokens())
            mutableSession.value = SessionState.LoggedIn

            LoginResult.Success
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: AuthenticationException) {
            if (exception.isCanceled) LoginResult.Cancelled else LoginResult.Failure
        } catch (exception: Exception) {
            LoginResult.Failure
        }
    }

    override suspend fun logout() {
        runCatching { tokenStore.clear() }
        mutableSession.value = SessionState.LoggedOut
    }

    override suspend fun accessToken(forceRefresh: Boolean): String? = refreshMutex.withLock {
        val tokens = tokenStore.read() ?: return@withLock null
        val valid = tokens.expiresAtEpochMillis - CLOCK_SKEW_MILLIS > System.currentTimeMillis()

        if (!forceRefresh && valid) {
            return@withLock tokens.accessToken
        }

        val refreshToken = tokens.refreshToken ?: return@withLock tokens.accessToken.takeIf { valid }

        runCatching {
            authentication.renewAuth(refreshToken = refreshToken, audience = config.audience).await()
        }.fold(
            onSuccess = { credentials ->
                tokenStore.write(credentials.toStoredTokens(fallbackRefreshToken = refreshToken))
                credentials.accessToken
            },
            onFailure = { exception ->
                if (exception is AuthenticationException && exception.isRefreshTokenDeleted) {
                    tokenStore.clear()
                    mutableSession.value = SessionState.LoggedOut
                }

                tokens.accessToken.takeIf { valid }
            },
        )
    }

    private fun Credentials.toStoredTokens(fallbackRefreshToken: String? = null): StoredTokens = StoredTokens(
        accessToken = accessToken,
        refreshToken = refreshToken ?: fallbackRefreshToken,
        expiresAtEpochMillis = expiresAt.time,
    )

    private companion object {
        const val GOOGLE_CONNECTION = "google-oauth2"
        const val SCOPE = "openid profile email offline_access"
        const val CLOCK_SKEW_MILLIS = 60_000L
    }
}

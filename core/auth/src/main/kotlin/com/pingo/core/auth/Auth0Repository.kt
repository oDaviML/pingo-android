package com.pingo.core.auth

import android.app.Activity
import android.util.Log
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
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
    private val googleSignIn: GoogleSignIn,
    private val dispatchers: AppDispatchers,
) : AuthRepository {
    private val account: Auth0? by lazy {
        if (config.domain.isNotBlank() && config.clientId.isNotBlank()) {
            Auth0.getInstance(config.clientId, config.domain)
        } else {
            null
        }
    }
    private val authentication: AuthenticationAPIClient? by lazy { account?.let(::AuthenticationAPIClient) }
    private val refreshMutex = Mutex()
    private val sessionScope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val mutableSession = MutableStateFlow(SessionState.Loading)

    override val session: StateFlow<SessionState> = mutableSession.asStateFlow()

    init {
        sessionScope.launch {
            val tokens = tokenStore.read()
            val usable = tokens != null && (
                tokens.refreshToken != null ||
                    tokens.expiresAtEpochMillis - CLOCK_SKEW_MILLIS > System.currentTimeMillis()
                )

            if (!usable) {
                tokenStore.clear()
            }

            mutableSession.value = if (usable) SessionState.LoggedIn else SessionState.LoggedOut
        }
    }

    override suspend fun loginWithGoogle(activity: Activity): LoginResult = withContext(dispatchers.io) {
        val client = authentication ?: return@withContext LoginResult.Failure

        when (val google = googleSignIn.signIn(activity)) {
            is GoogleSignInResult.Success -> loginWithGoogleIdToken(client, google.idToken)

            GoogleSignInResult.Cancelled -> LoginResult.Cancelled

            GoogleSignInResult.Failure -> LoginResult.Failure
        }
    }

    private suspend fun loginWithGoogleIdToken(client: AuthenticationAPIClient, idToken: String): LoginResult = try {
        val credentials = client.loginWithNativeSocialToken(idToken, GOOGLE_ID_TOKEN_TYPE)
            .validateClaims()
            .setScope(SCOPE)
            .setAudience(config.audience)
            .await()

        tokenStore.write(credentials.toStoredTokens())
        mutableSession.value = SessionState.LoggedIn

        LoginResult.Success
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: AuthenticationException) {
        Log.w(TAG, "Falha no login Auth0 (${exception.getCode()}): ${exception.getDescription()}")

        if (exception.isCanceled) LoginResult.Cancelled else LoginResult.Failure
    } catch (exception: Exception) {
        Log.w(TAG, "Falha no login: ${exception.javaClass.simpleName}: ${exception.message}")
        LoginResult.Failure
    }

    override suspend fun logout() {
        runCatching { tokenStore.clear() }
        mutableSession.value = SessionState.LoggedOut
    }

    override suspend fun accessToken(forceRefresh: Boolean): String? = refreshMutex.withLock {
        val client = authentication ?: return@withLock null
        val tokens = tokenStore.read() ?: return@withLock null
        val valid = tokens.expiresAtEpochMillis - CLOCK_SKEW_MILLIS > System.currentTimeMillis()

        if (!forceRefresh && valid) {
            return@withLock tokens.accessToken
        }

        val refreshToken = tokens.refreshToken

        if (refreshToken == null) {
            if (valid) {
                return@withLock tokens.accessToken
            }

            tokenStore.clear()
            mutableSession.value = SessionState.LoggedOut

            return@withLock null
        }

        runCatching {
            client.renewAuth(refreshToken = refreshToken, audience = config.audience).await()
        }.fold(
            onSuccess = { credentials ->
                tokenStore.write(credentials.toStoredTokens(fallbackRefreshToken = refreshToken))
                credentials.accessToken
            },
            onFailure = { exception ->
                if (exception is AuthenticationException && exception.invalidatesSession()) {
                    tokenStore.clear()
                    mutableSession.value = SessionState.LoggedOut
                }

                tokens.accessToken.takeIf { valid }
            },
        )
    }

    private fun AuthenticationException.invalidatesSession(): Boolean = isRefreshTokenDeleted || isInvalidRefreshToken

    private fun Credentials.toStoredTokens(fallbackRefreshToken: String? = null): StoredTokens = StoredTokens(
        accessToken = accessToken,
        refreshToken = refreshToken ?: fallbackRefreshToken,
        expiresAtEpochMillis = expiresAt.time,
    )

    private companion object {
        const val TAG = "PingoAuth"
        const val GOOGLE_ID_TOKEN_TYPE = "http://auth0.com/oauth/token-type/google-id-token"
        const val SCOPE = "openid profile email offline_access"
        const val CLOCK_SKEW_MILLIS = 60_000L
    }
}

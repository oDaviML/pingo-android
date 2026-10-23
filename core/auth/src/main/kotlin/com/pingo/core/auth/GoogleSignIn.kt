package com.pingo.core.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.pingo.core.common.AppDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject

sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult

    data object Cancelled : GoogleSignInResult

    data object Failure : GoogleSignInResult
}

interface GoogleSignIn {
    suspend fun signIn(context: Context): GoogleSignInResult
}

class CredentialManagerGoogleSignIn @Inject constructor(private val config: Auth0Config, private val dispatchers: AppDispatchers) :
    GoogleSignIn {
    override suspend fun signIn(context: Context): GoogleSignInResult = withContext(dispatchers.io) {
        if (config.googleWebClientId.isBlank()) {
            return@withContext GoogleSignInResult.Failure
        }

        try {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(config.googleWebClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setNonce(nonce())
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val credential = CredentialManager.create(context)
                .getCredential(context, request)
                .credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleSignInResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleSignInResult.Failure
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (exception: Exception) {
            Log.w(TAG, "Falha no Credential Manager: ${exception.javaClass.simpleName}: ${exception.message}")
            GoogleSignInResult.Failure
        }
    }

    private fun nonce(): String {
        val bytes = ByteArray(NONCE_BYTES)
        SecureRandom().nextBytes(bytes)

        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    private companion object {
        const val TAG = "PingoAuth"
        const val NONCE_BYTES = 16
    }
}

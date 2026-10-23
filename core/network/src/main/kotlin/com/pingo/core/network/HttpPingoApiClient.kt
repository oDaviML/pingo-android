package com.pingo.core.network

import com.pingo.core.auth.AuthRepository
import com.pingo.core.common.AppDispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpPingoApiClient @Inject constructor(
    config: PingoApiConfig,
    private val authRepository: AuthRepository,
    private val dispatchers: AppDispatchers,
) : PingoApiClient {
    private val baseUrl = config.baseUrl.trimEnd('/')
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(config.connectTimeoutMillis, TimeUnit.MILLISECONDS)
        .build()

    override suspend fun healthCheck(): Boolean = withContext(dispatchers.io) {
        runCatching {
            client.newCall(Request.Builder().url("$baseUrl/health").get().build()).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    override suspend fun getProfile(): ApiProfile = withContext(dispatchers.io) {
        val body = executeAuthenticated { token ->
            Request.Builder()
                .url("$baseUrl/account/profile")
                .header(AUTHORIZATION_HEADER, "Bearer $token")
                .get()
                .build()
        }

        json.decodeFromString<ProfileResponse>(body).toModel()
    }

    private suspend fun executeAuthenticated(request: (String) -> Request): String {
        val token = authRepository.accessToken() ?: throw PingoApiException(UNAUTHORIZED, "not_authenticated")
        val response = execute(request(token))

        if (response.code == UNAUTHORIZED) {
            val refreshed = authRepository.accessToken(forceRefresh = true)
                ?: throw PingoApiException(UNAUTHORIZED, "not_authenticated")

            return readBody(execute(request(refreshed)))
        }

        return readBody(response)
    }

    private fun execute(request: Request): Response = client.newCall(request).execute()

    private fun readBody(response: Response): String {
        val body = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw PingoApiException(response.code, body)
        }

        return body
    }

    private fun ProfileResponse.toModel() = ApiProfile(
        email = email,
        nome = nome,
        consentimentoLgpdEm = consentimentoLgpdEm,
        consentimentoLgpdVersao = consentimentoLgpdVersao,
        aparelhoAtivoId = aparelhoAtivoId,
        aparelhoAtivoNome = aparelhoAtivoNome,
        aparelhoAtivoEm = aparelhoAtivoEm,
    )

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val UNAUTHORIZED = 401
    }
}

@Serializable
private data class ProfileResponse(
    val email: String,
    val nome: String? = null,
    val consentimentoLgpdEm: String? = null,
    val consentimentoLgpdVersao: String? = null,
    val aparelhoAtivoId: String? = null,
    val aparelhoAtivoNome: String? = null,
    val aparelhoAtivoEm: String? = null,
)

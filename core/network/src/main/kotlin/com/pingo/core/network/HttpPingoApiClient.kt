package com.pingo.core.network

import com.pingo.core.auth.AuthRepository
import com.pingo.core.common.AppDispatchers
import com.pingo.core.network.sync.PullChangesRequest
import com.pingo.core.network.sync.PullChangesResult
import com.pingo.core.network.sync.PushChangesPayload
import com.pingo.core.network.sync.PushChangesResult
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    override suspend fun pushChanges(payload: PushChangesPayload): PushChangesResult = withContext(dispatchers.io) {
        val body = executeAuthenticated { token ->
            postJson(
                url = "$baseUrl/sync/push",
                token = token,
                body = json.encodeToString(PushChangesPayload.serializer(), payload),
            )
        }

        json.decodeFromString(PushChangesResult.serializer(), body)
    }

    override suspend fun pullChanges(cursor: String?): PullChangesResult = withContext(dispatchers.io) {
        val body = executeAuthenticated { token ->
            postJson(
                url = "$baseUrl/sync/pull",
                token = token,
                body = json.encodeToString(PullChangesRequest.serializer(), PullChangesRequest(cursor)),
            )
        }

        json.decodeFromString(PullChangesResult.serializer(), body)
    }

    override suspend fun activateDevice(deviceId: String, deviceName: String?): ApiDevice = withContext(dispatchers.io) {
        val body = executeAuthenticated { token ->
            postJson(
                url = "$baseUrl/account/devices/activate",
                token = token,
                body = json.encodeToString(
                    ActivateDeviceRequestDto.serializer(),
                    ActivateDeviceRequestDto(deviceId = deviceId, deviceName = deviceName),
                ),
            )
        }

        json.decodeFromString<ActivateDeviceResponseDto>(body).toModel()
    }

    override suspend fun recordConsent(version: String): ApiConsent = withContext(dispatchers.io) {
        val body = executeAuthenticated { token ->
            postJson(
                url = "$baseUrl/account/consent",
                token = token,
                body = json.encodeToString(
                    RecordConsentRequestDto.serializer(),
                    RecordConsentRequestDto(versao = version),
                ),
            )
        }

        val consent = json.decodeFromString<RecordConsentResponseDto>(body)

        ApiConsent(consentedAt = consent.consentidoEm, version = consent.versao)
    }

    override suspend fun deleteAccount(): Unit = withContext(dispatchers.io) {
        executeAuthenticated { token ->
            Request.Builder()
                .url("$baseUrl/account")
                .header(AUTHORIZATION_HEADER, "Bearer $token")
                .delete()
                .build()
        }

        Unit
    }

    private fun postJson(url: String, token: String, body: String): Request = Request.Builder()
        .url(url)
        .header(AUTHORIZATION_HEADER, "Bearer $token")
        .post(body.toRequestBody(JSON_MEDIA_TYPE))
        .build()

    private suspend fun executeAuthenticated(request: (String) -> Request): String {
        val token = authRepository.accessToken() ?: throw PingoApiException(UNAUTHORIZED, "not_authenticated")
        val response = execute(request(token))

        if (response.code == UNAUTHORIZED) {
            response.close()

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

    private fun ActivateDeviceResponseDto.toModel() = ApiDevice(
        deviceId = deviceId,
        deviceName = deviceName,
        activatedAt = activatedAt,
    )

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val UNAUTHORIZED = 401
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
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

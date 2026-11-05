package com.pingo.core.network

import android.app.Activity
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.common.DefaultAppDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.DataInputStream
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

class HttpPingoApiClientTest {
    @Test
    fun `envia o token como bearer ao ler o perfil`() {
        val servidor = ServidorDeUmaRequisicao(corpo = """{"email":"lucas@pingo.app"}""")
        val client = HttpPingoApiClient(
            config = PingoApiConfig(baseUrl = servidor.baseUrl),
            authRepository = FakeAuthRepository(token = "token-123"),
            dispatchers = DefaultAppDispatchers,
        )

        runBlocking { client.getProfile() }

        assertEquals("Bearer token-123", servidor.authorization())
    }
}

private class ServidorDeUmaRequisicao(private val corpo: String) {
    private val servidor = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
    private var authorization: String? = null
    private val atendimento = thread(isDaemon = true) { atende() }
    val baseUrl = "http://127.0.0.1:${servidor.localPort}"

    fun authorization(): String? {
        atendimento.join()
        servidor.close()
        return authorization
    }

    private fun atende() {
        servidor.accept().use { conexao ->
            authorization = lerCabecalhos(DataInputStream(conexao.getInputStream())).valorDe("Authorization")
            conexao.getOutputStream().write(respostaHttp().toByteArray())
        }
    }

    private fun lerCabecalhos(entrada: DataInputStream): List<String> = buildString {
        while (!endsWith("\r\n\r\n")) append(entrada.readByte().toInt().toChar())
    }.lines()

    private fun List<String>.valorDe(nome: String): String? =
        firstOrNull { it.startsWith("$nome:", ignoreCase = true) }?.substringAfter(':')?.trim()

    private fun respostaHttp(): String {
        val cabecalhos = "HTTP/1.1 200 OK\r\nContent-Length: ${corpo.length}\r\nConnection: close"
        return "$cabecalhos\r\n\r\n$corpo"
    }
}

private class FakeAuthRepository(private val token: String) : AuthRepository {
    override val session: StateFlow<SessionState> = MutableStateFlow(SessionState.LoggedIn)

    override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Success

    override suspend fun logout() = Unit

    override suspend fun accessToken(forceRefresh: Boolean): String? = token
}

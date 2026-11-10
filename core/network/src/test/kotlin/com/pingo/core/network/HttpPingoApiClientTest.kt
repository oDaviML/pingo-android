package com.pingo.core.network

import android.app.Activity
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.common.DefaultAppDispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    @Test
    fun `baixa as regras vigentes do parser com o token bearer`() {
        val servidor = ServidorDeUmaRequisicao(
            corpo = """
                {
                  "versao": 1,
                  "regras": [
                    {
                      "publicId": "00000000-0000-0000-0000-000000000001",
                      "instituicao": "Nubank",
                      "versao": 1,
                      "publicadaEm": "2026-11-10T00:00:00+00:00",
                      "padroes": [
                        {"id": "nubank-compra", "tipoLancamento": "COMPRA", "ancoras": ["Compra de R$"], "extracoes": {"valor": "Compra de R$ {valor}"}}
                      ]
                    }
                  ]
                }
            """.trimIndent(),
        )
        val client = HttpPingoApiClient(
            config = PingoApiConfig(baseUrl = servidor.baseUrl),
            authRepository = FakeAuthRepository(token = "token-123"),
            dispatchers = DefaultAppDispatchers,
        )

        val resposta = runBlocking { client.getParserRules() }

        assertEquals("Bearer token-123", servidor.authorization())
        assertEquals("/parser-rules/latest", servidor.caminho())
        assertEquals(1, resposta.versao)
        val regra = resposta.regras.single()
        assertEquals("Nubank", regra.instituicao)
        assertEquals(1, regra.versao)
        assertEquals("nubank-compra", regra.padroes.single().jsonObject.getValue("id").jsonPrimitive.content)
    }
}

private class ServidorDeUmaRequisicao(private val corpo: String) {
    private val servidor = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
    private var authorization: String? = null
    private var caminhoRequisitado: String? = null
    private val atendimento = thread(isDaemon = true) { atende() }
    val baseUrl = "http://127.0.0.1:${servidor.localPort}"

    fun authorization(): String? {
        atendimento.join()
        servidor.close()
        return authorization
    }

    fun caminho(): String? {
        atendimento.join()
        return caminhoRequisitado
    }

    private fun atende() {
        servidor.accept().use { conexao ->
            val cabecalhos = lerCabecalhos(DataInputStream(conexao.getInputStream()))
            caminhoRequisitado = cabecalhos.firstOrNull()?.substringAfter(' ')?.substringBefore(' ')
            authorization = cabecalhos.valorDe("Authorization")
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

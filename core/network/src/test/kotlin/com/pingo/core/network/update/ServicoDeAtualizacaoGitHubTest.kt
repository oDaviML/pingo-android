package com.pingo.core.network.update

import com.pingo.core.common.DefaultAppDispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

class ServicoDeAtualizacaoGitHubTest {
    @Test
    fun `retorna nova versao disponivel quando release mais recente supera versao instalada`() = runBlocking {
        val corpo = """
            {
              "tag_name": "v0.3.0",
              "name": "Pingo 0.3.0 — Beta",
              "body": "- Biometria e PIN\n- Icone do app",
              "html_url": "https://github.com/oDaviML/pingo-android/releases/tag/v0.3.0",
              "draft": false,
              "assets": [
                {
                  "name": "Pingo-0.3.0-release.apk",
                  "size": 21500000,
                  "browser_download_url": "http://127.0.0.1:9999/Pingo-0.3.0-release.apk"
                },
                {
                  "name": "Pingo-0.3.0-release.apk.sha256",
                  "size": 90,
                  "browser_download_url": "http://127.0.0.1:9999/Pingo-0.3.0-release.apk.sha256"
                }
              ]
            }
        """.trimIndent()
        val servidor = ServidorHttpRelease(status = "200 OK", corpo = corpo)
        val servico = ServicoDeAtualizacaoGitHub(
            dispatchers = DefaultAppDispatchers,
            config = ConfiguracaoAtualizacaoApk(urlLatestRelease = servidor.url),
        )

        val resultado = servico.verificar("0.2.0")

        assertTrue(resultado is ResultadoVerificacaoAtualizacao.NovaVersaoDisponivel)
        val info = (resultado as ResultadoVerificacaoAtualizacao.NovaVersaoDisponivel).info
        assertEquals("0.3.0", info.versao)
        assertEquals("v0.3.0", info.tag)
        assertEquals("Pingo 0.3.0 — Beta", info.titulo)
        assertEquals("Pingo-0.3.0-release.apk", info.nomeArquivoApk)
        assertEquals("http://127.0.0.1:9999/Pingo-0.3.0-release.apk", info.urlDownloadApk)
        assertEquals("http://127.0.0.1:9999/Pingo-0.3.0-release.apk.sha256", info.urlSha256)
        assertEquals("application/vnd.github+json", servidor.ultimoAccept())
    }

    @Test
    fun `retorna atualizado quando versao instalada iguala ou supera ultima release`() = runBlocking {
        val corpo = """
            {
              "tag_name": "v0.2.0",
              "name": "Pingo 0.2.0",
              "assets": []
            }
        """.trimIndent()
        val servidor = ServidorHttpRelease(status = "200 OK", corpo = corpo)
        val servico = ServicoDeAtualizacaoGitHub(
            dispatchers = DefaultAppDispatchers,
            config = ConfiguracaoAtualizacaoApk(urlLatestRelease = servidor.url),
        )

        val resultado = servico.verificar("0.2.0")

        assertEquals(ResultadoVerificacaoAtualizacao.Atualizado("0.2.0"), resultado)
    }

    @Test
    fun `retorna sem rede quando servidor esta inacessivel`() = runBlocking {
        val portaFechada = ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { it.localPort }
        val servico = ServicoDeAtualizacaoGitHub(
            dispatchers = DefaultAppDispatchers,
            config = ConfiguracaoAtualizacaoApk(
                urlLatestRelease = "http://127.0.0.1:$portaFechada/releases/latest",
                connectTimeoutMillis = 500L,
                readTimeoutMillis = 500L,
            ),
        )

        val resultado = servico.verificar("0.2.0")

        assertEquals(ResultadoVerificacaoAtualizacao.SemRede, resultado)
    }

    @Test
    fun `retorna indisponivel quando servidor responde erro http`() = runBlocking {
        val servidor = ServidorHttpRelease(status = "503 Service Unavailable", corpo = """{"message":"unavailable"}""")
        val servico = ServicoDeAtualizacaoGitHub(
            dispatchers = DefaultAppDispatchers,
            config = ConfiguracaoAtualizacaoApk(urlLatestRelease = servidor.url),
        )

        val resultado = servico.verificar("0.2.0")

        assertEquals(ResultadoVerificacaoAtualizacao.Indisponivel, resultado)
    }

    private class ServidorHttpRelease(private val status: String, private val corpo: String) {
        private val socket = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        private var acceptHeader: String? = null
        private val threadServidor = thread(isDaemon = true) {
            socket.accept().use { conexao ->
                val entrada = DataInputStream(conexao.getInputStream())
                val linhas = buildString {
                    while (!endsWith("\r\n\r\n")) append(entrada.readByte().toInt().toChar())
                }.lines()
                acceptHeader = linhas.firstOrNull { it.startsWith("Accept:", ignoreCase = true) }
                    ?.substringAfter(':')
                    ?.trim()
                val bytes = corpo.toByteArray(Charsets.UTF_8)
                val header = "HTTP/1.1 $status\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: ${bytes.size}\r\n" +
                    "Connection: close\r\n\r\n"
                conexao.getOutputStream().apply {
                    write(header.toByteArray(Charsets.UTF_8))
                    write(bytes)
                }
            }
            socket.close()
        }
        val url = "http://127.0.0.1:${socket.localPort}/releases/latest"

        fun ultimoAccept(): String? {
            threadServidor.join(3000)
            return acceptHeader
        }
    }
}

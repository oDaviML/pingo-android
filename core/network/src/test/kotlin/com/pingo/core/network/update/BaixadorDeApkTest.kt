package com.pingo.core.network.update

import com.pingo.core.common.DefaultAppDispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.concurrent.thread

class BaixadorDeApkTest {
    @Test
    fun `baixa apk e valida sha256 com sucesso`() = runBlocking {
        val dirCache = Files.createTempDirectory("pingo-apk-test-ok").toFile()
        try {
            val payloadApk = "conteudo-binario-simulado-apk-v0.3.0".toByteArray(Charsets.UTF_8)
            val hashEsperado = sha256Hex(payloadApk)
            val servidor = ServidorMultiArquivos(
                rotas = mapOf(
                    "/Pingo-0.3.0-release.apk" to payloadApk,
                    "/Pingo-0.3.0-release.apk.sha256" to "$hashEsperado  Pingo-0.3.0-release.apk\n".toByteArray(Charsets.UTF_8),
                ),
            )
            val baixador = BaixadorDeApk(dirCache, DefaultAppDispatchers)

            val estados = baixador.baixar(
                versao = "0.3.0",
                urlApk = "${servidor.baseUrl}/Pingo-0.3.0-release.apk",
                urlSha256 = "${servidor.baseUrl}/Pingo-0.3.0-release.apk.sha256",
            ).toList()

            servidor.fechar()
            assertTrue(estados.any { it is EstadoDownloadApk.EmProgresso })
            val concluido = estados.last() as EstadoDownloadApk.Concluido
            assertTrue(concluido.arquivoApk.exists())
            assertArrayEquals(payloadApk, concluido.arquivoApk.readBytes())
        } finally {
            dirCache.deleteRecursively()
        }
    }

    @Test
    fun `rejeita e apaga apk quando sha256 diverge`() = runBlocking {
        val dirCache = Files.createTempDirectory("pingo-apk-test-bad").toFile()
        try {
            val payloadApk = "conteudo-corrompido".toByteArray(Charsets.UTF_8)
            val hashIncorreto = "a".repeat(64)
            val servidor = ServidorMultiArquivos(
                rotas = mapOf(
                    "/Pingo-0.3.0-release.apk" to payloadApk,
                    "/Pingo-0.3.0-release.apk.sha256" to "$hashIncorreto  Pingo-0.3.0-release.apk\n".toByteArray(Charsets.UTF_8),
                ),
            )
            val baixador = BaixadorDeApk(dirCache, DefaultAppDispatchers)

            val estados = baixador.baixar(
                versao = "0.3.0",
                urlApk = "${servidor.baseUrl}/Pingo-0.3.0-release.apk",
                urlSha256 = "${servidor.baseUrl}/Pingo-0.3.0-release.apk.sha256",
            ).toList()

            servidor.fechar()
            val falha = estados.last() as EstadoDownloadApk.Falha
            assertEquals(MotivoFalhaDownloadApk.INTEGRIDADE_INVALIDA, falha.motivo)
            val pastaUpdates = File(dirCache, "apk_updates")
            assertFalse(File(pastaUpdates, "Pingo-0.3.0.apk").exists())
        } finally {
            dirCache.deleteRecursively()
        }
    }

    @Test
    fun `rejeita url http externa nao autorizada`() = runBlocking {
        val dirCache = Files.createTempDirectory("pingo-apk-test-url").toFile()
        try {
            val baixador = BaixadorDeApk(dirCache, DefaultAppDispatchers)

            val estados = baixador.baixar(
                versao = "0.3.0",
                urlApk = "http://malicioso.exemplo.com/Pingo.apk",
                urlSha256 = null,
            ).toList()

            val falha = estados.last() as EstadoDownloadApk.Falha
            assertEquals(MotivoFalhaDownloadApk.URL_INVALIDA, falha.motivo)
        } finally {
            dirCache.deleteRecursively()
        }
    }

    private fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString(separator = "") { "%02x".format(it) }

    private class ServidorMultiArquivos(private val rotas: Map<String, ByteArray>) {
        private val socket = ServerSocket(0, 8, InetAddress.getLoopbackAddress())

        @Volatile
        private var ativo = true
        val baseUrl = "http://127.0.0.1:${socket.localPort}"

        init {
            thread(isDaemon = true) {
                while (ativo) {
                    val conexao = runCatching { socket.accept() }.getOrNull() ?: break
                    conexao.use { s ->
                        val entrada = DataInputStream(s.getInputStream())
                        val linhas = buildString {
                            while (!endsWith("\r\n\r\n")) append(entrada.readByte().toInt().toChar())
                        }.lines()
                        val caminho = linhas.firstOrNull()?.substringAfter(' ')?.substringBefore(' ')
                        val payload = rotas[caminho]
                        if (payload != null) {
                            val header = "HTTP/1.1 200 OK\r\nContent-Length: ${payload.size}\r\nConnection: close\r\n\r\n"
                            s.getOutputStream().apply {
                                write(header.toByteArray(Charsets.UTF_8))
                                write(payload)
                            }
                        } else {
                            val naoEncontrado = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                            s.getOutputStream().write(naoEncontrado.toByteArray())
                        }
                    }
                }
            }
        }

        fun fechar() {
            ativo = false
            socket.close()
        }
    }
}

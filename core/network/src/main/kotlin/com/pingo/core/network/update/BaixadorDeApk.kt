package com.pingo.core.network.update

import com.pingo.core.common.AppDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class BaixadorDeApk(
    private val diretorioCache: File,
    private val dispatchers: AppDispatchers,
    private val client: OkHttpClient = criarClientePadrao(),
) {
    fun baixar(atualizacao: AtualizacaoDisponivel): Flow<EstadoDownloadApk> = baixar(
        versao = atualizacao.versao,
        urlApk = atualizacao.urlDownloadApk,
        urlSha256 = atualizacao.urlSha256,
    )

    fun baixar(versao: String, urlApk: String, urlSha256: String? = null): Flow<EstadoDownloadApk> = flow {
        if (!ehUrlAtualizacaoPermitida(urlApk) || (urlSha256 != null && !ehUrlAtualizacaoPermitida(urlSha256))) {
            emit(EstadoDownloadApk.Falha(MotivoFalhaDownloadApk.URL_INVALIDA))
            return@flow
        }

        val arquivoDestino = prepararArquivoDestino(versao)
        if (arquivoDestino == null) {
            emit(EstadoDownloadApk.Falha(MotivoFalhaDownloadApk.ERRO_DISCO))
            return@flow
        }

        var concluido = false
        try {
            val hashCalculado = executarDownloadComProgresso(
                urlApk = urlApk,
                arquivoDestino = arquivoDestino,
                emitirProgresso = { percentual -> emit(EstadoDownloadApk.EmProgresso(percentual)) },
            )
            if (urlSha256 != null && !validarSha256(urlSha256, hashCalculado)) {
                arquivoDestino.delete()
                emit(EstadoDownloadApk.Falha(MotivoFalhaDownloadApk.INTEGRIDADE_INVALIDA))
                return@flow
            }
            concluido = true
            emit(EstadoDownloadApk.Concluido(arquivoDestino))
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (falha: FalhaDownloadInternaException) {
            emit(EstadoDownloadApk.Falha(falha.motivo, falha.message))
        } catch (erro: IOException) {
            emit(EstadoDownloadApk.Falha(MotivoFalhaDownloadApk.SEM_REDE, erro.message))
        } finally {
            if (!concluido) {
                arquivoDestino.delete()
            }
        }
    }.flowOn(dispatchers.io)

    private fun prepararArquivoDestino(versao: String): File? {
        val pastaUpdates = File(diretorioCache, SUBDIRETORIO_APK_UPDATES)
        if (!pastaUpdates.exists() && !pastaUpdates.mkdirs()) {
            return null
        }
        val versaoSanitizada = ComparadorDeVersao.normalizar(versao)
            .replace(Regex("[^0-9A-Za-z._-]"), "_")
            .ifBlank { "atualizacao" }
        val arquivo = File(pastaUpdates, "Pingo-$versaoSanitizada.apk")
        if (arquivo.exists() && !arquivo.delete()) {
            return null
        }
        return arquivo
    }

    private suspend fun executarDownloadComProgresso(
        urlApk: String,
        arquivoDestino: File,
        emitirProgresso: suspend (Int) -> Unit,
    ): String {
        val request = Request.Builder().url(urlApk).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful || !ehHttpUrlAtualizacaoPermitida(response.request.url)) {
                throw FalhaDownloadInternaException(MotivoFalhaDownloadApk.ERRO_SERVIDOR)
            }
            val body = response.body ?: throw FalhaDownloadInternaException(MotivoFalhaDownloadApk.ERRO_SERVIDOR)
            return gravarStreamCalculandoSha256(body, arquivoDestino, emitirProgresso)
        }
    }

    private suspend fun gravarStreamCalculandoSha256(
        body: ResponseBody,
        arquivoDestino: File,
        emitirProgresso: suspend (Int) -> Unit,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        emitirProgresso(0)
        var ultimoPercentual = 0

        body.byteStream().use { entrada ->
            abrirOutputSeguro(arquivoDestino).use { saida ->
                ultimoPercentual = copiarFluxoCalculandoHash(
                    entrada = entrada,
                    saida = saida,
                    digest = digest,
                    totalBytes = body.contentLength(),
                    ultimoPercentualInicial = ultimoPercentual,
                    emitirProgresso = emitirProgresso,
                )
                saida.fd.sync()
            }
        }

        if (ultimoPercentual < 100) {
            emitirProgresso(100)
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private suspend fun copiarFluxoCalculandoHash(
        entrada: java.io.InputStream,
        saida: FileOutputStream,
        digest: MessageDigest,
        totalBytes: Long,
        ultimoPercentualInicial: Int,
        emitirProgresso: suspend (Int) -> Unit,
    ): Int {
        val buffer = ByteArray(TAMANHO_BUFFER_BYTES)
        var bytesBaixados = 0L
        var ultimoPercentual = ultimoPercentualInicial
        var lidos = entrada.read(buffer)

        while (lidos >= 0) {
            digest.update(buffer, 0, lidos)
            gravarBytesSeguro(saida, buffer, lidos)
            bytesBaixados += lidos
            val percentual = calcularPercentual(bytesBaixados, totalBytes)
            if (percentual != null && percentual != ultimoPercentual) {
                ultimoPercentual = percentual
                emitirProgresso(percentual)
            }
            lidos = entrada.read(buffer)
        }

        return ultimoPercentual
    }

    private fun abrirOutputSeguro(arquivoDestino: File): FileOutputStream = try {
        FileOutputStream(arquivoDestino)
    } catch (erro: IOException) {
        throw FalhaDownloadInternaException(MotivoFalhaDownloadApk.ERRO_DISCO, erro.message)
    }

    private fun gravarBytesSeguro(saida: FileOutputStream, buffer: ByteArray, tamanho: Int) {
        try {
            saida.write(buffer, 0, tamanho)
        } catch (erro: IOException) {
            throw FalhaDownloadInternaException(MotivoFalhaDownloadApk.ERRO_DISCO, erro.message)
        }
    }

    private fun calcularPercentual(bytesBaixados: Long, totalBytes: Long): Int? {
        if (totalBytes <= 0L) return null
        return ((bytesBaixados * 100L) / totalBytes).toInt().coerceIn(0, 100)
    }

    private fun validarSha256(urlSha256: String, hashCalculado: String): Boolean {
        val request = Request.Builder().url(urlSha256).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful || !ehHttpUrlAtualizacaoPermitida(response.request.url)) {
                throw FalhaDownloadInternaException(MotivoFalhaDownloadApk.ERRO_SERVIDOR)
            }
            val textoSha256 = response.body?.string().orEmpty()
            val hashEsperado = extrairSha256Hex(textoSha256) ?: return false
            return MessageDigest.isEqual(
                hashCalculado.lowercase().toByteArray(Charsets.US_ASCII),
                hashEsperado.toByteArray(Charsets.US_ASCII),
            )
        }
    }

    private fun extrairSha256Hex(conteudo: String): String? = conteudo
        .trim()
        .removePrefix("\uFEFF")
        .split(REGEX_ESPACOS)
        .firstOrNull()
        ?.lowercase()
        ?.takeIf { REGEX_SHA256_HEX.matches(it) }

    private class FalhaDownloadInternaException(val motivo: MotivoFalhaDownloadApk, detail: String? = null) : IOException(detail)

    companion object {
        private const val SUBDIRETORIO_APK_UPDATES = "apk_updates"
        private const val TAMANHO_BUFFER_BYTES = 8 * 1024
        private val REGEX_ESPACOS = Regex("\\s+")
        private val REGEX_SHA256_HEX = Regex("^[0-9a-f]{64}$")

        private fun criarClientePadrao(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .build()
    }
}

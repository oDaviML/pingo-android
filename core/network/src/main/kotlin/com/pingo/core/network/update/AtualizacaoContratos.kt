package com.pingo.core.network.update

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File

data class AtualizacaoDisponivel(
    val versao: String,
    val tag: String,
    val titulo: String,
    val notas: String,
    val urlPaginaRelease: String,
    val urlDownloadApk: String,
    val nomeArquivoApk: String,
    val tamanhoBytes: Long? = null,
    val urlSha256: String? = null,
)

sealed interface ResultadoVerificacaoAtualizacao {
    data class Atualizado(val versaoAtual: String) : ResultadoVerificacaoAtualizacao

    data class NovaVersaoDisponivel(val info: AtualizacaoDisponivel) : ResultadoVerificacaoAtualizacao

    data object SemRede : ResultadoVerificacaoAtualizacao

    data object Indisponivel : ResultadoVerificacaoAtualizacao
}

sealed interface EstadoDownloadApk {
    data class EmProgresso(val percentual: Int) : EstadoDownloadApk

    data class Concluido(val arquivoApk: File) : EstadoDownloadApk

    data class Falha(val motivo: MotivoFalhaDownloadApk, val detalhe: String? = null) : EstadoDownloadApk
}

enum class MotivoFalhaDownloadApk {
    URL_INVALIDA,
    SEM_REDE,
    ERRO_SERVIDOR,
    INTEGRIDADE_INVALIDA,
    ERRO_DISCO,
}

data class ConfiguracaoAtualizacaoApk(
    val urlLatestRelease: String = URL_GITHUB_LATEST_RELEASE,
    val connectTimeoutMillis: Long = 15_000L,
    val readTimeoutMillis: Long = 30_000L,
) {
    companion object {
        const val URL_GITHUB_LATEST_RELEASE =
            "https://api.github.com/repos/oDaviML/pingo-android/releases/latest"
    }
}

interface VerificadorDeAtualizacaoApk {
    suspend fun verificar(versaoInstalada: String): ResultadoVerificacaoAtualizacao
}

internal fun ehUrlAtualizacaoPermitida(url: String): Boolean {
    val parsed = url.trim().toHttpUrlOrNull() ?: return false
    return ehHttpUrlAtualizacaoPermitida(parsed)
}

internal fun ehHttpUrlAtualizacaoPermitida(url: HttpUrl): Boolean = when (url.scheme) {
    "https" -> true
    "http" -> url.host in setOf("localhost", "127.0.0.1", "::1")
    else -> false
}

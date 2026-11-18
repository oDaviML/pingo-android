package com.pingo.core.network.update

import com.pingo.core.common.AppDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServicoDeAtualizacaoGitHub @Inject constructor(
    private val dispatchers: AppDispatchers,
    private val config: ConfiguracaoAtualizacaoApk,
) : VerificadorDeAtualizacaoApk {
    private val client = OkHttpClient.Builder()
        .connectTimeout(config.connectTimeoutMillis, TimeUnit.MILLISECONDS)
        .readTimeout(config.readTimeoutMillis, TimeUnit.MILLISECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun verificar(versaoInstalada: String): ResultadoVerificacaoAtualizacao = withContext(dispatchers.io) {
        if (!ehUrlAtualizacaoPermitida(config.urlLatestRelease)) {
            return@withContext ResultadoVerificacaoAtualizacao.Indisponivel
        }
        if (ComparadorDeVersao.parse(versaoInstalada) == null) {
            return@withContext ResultadoVerificacaoAtualizacao.Indisponivel
        }

        try {
            executarConsulta(versaoInstalada)
        } catch (cancelamento: CancellationException) {
            throw cancelamento
        } catch (_: IOException) {
            ResultadoVerificacaoAtualizacao.SemRede
        } catch (_: SerializationException) {
            ResultadoVerificacaoAtualizacao.Indisponivel
        } catch (_: IllegalArgumentException) {
            ResultadoVerificacaoAtualizacao.Indisponivel
        }
    }

    private fun executarConsulta(versaoInstalada: String): ResultadoVerificacaoAtualizacao {
        val request = Request.Builder()
            .url(config.urlLatestRelease)
            .header("Accept", HEADER_ACCEPT_GITHUB)
            .header("User-Agent", HEADER_USER_AGENT)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful || !ehHttpUrlAtualizacaoPermitida(response.request.url)) {
                return ResultadoVerificacaoAtualizacao.Indisponivel
            }
            val body = response.body?.string()?.takeIf { it.isNotBlank() }
                ?: return ResultadoVerificacaoAtualizacao.Indisponivel
            val release = json.decodeFromString(GitHubReleaseDto.serializer(), body)
            return mapearRelease(release, versaoInstalada)
        }
    }

    private fun mapearRelease(release: GitHubReleaseDto, versaoInstalada: String): ResultadoVerificacaoAtualizacao {
        val versaoRemota = if (release.draft) null else ComparadorDeVersao.parse(release.tagName)
        val versaoLocal = ComparadorDeVersao.parse(versaoInstalada)
        if (versaoRemota == null || versaoLocal == null) {
            return ResultadoVerificacaoAtualizacao.Indisponivel
        }
        if (versaoRemota <= versaoLocal) {
            return ResultadoVerificacaoAtualizacao.Atualizado(
                versaoAtual = ComparadorDeVersao.normalizar(versaoInstalada),
            )
        }
        return criarAtualizacaoDisponivel(release) ?: ResultadoVerificacaoAtualizacao.Indisponivel
    }

    private fun criarAtualizacaoDisponivel(release: GitHubReleaseDto): ResultadoVerificacaoAtualizacao.NovaVersaoDisponivel? {
        val apkAsset = selecionarAssetApk(release.assets) ?: return null
        val sha256Asset = selecionarAssetSha256(release.assets, apkAsset.name)
            ?.takeIf { ehUrlAtualizacaoPermitida(it.browserDownloadUrl) }

        return ResultadoVerificacaoAtualizacao.NovaVersaoDisponivel(
            AtualizacaoDisponivel(
                versao = ComparadorDeVersao.normalizar(release.tagName),
                tag = release.tagName,
                titulo = release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
                notas = release.body ?: "",
                urlPaginaRelease = release.htmlUrl,
                urlDownloadApk = apkAsset.browserDownloadUrl,
                nomeArquivoApk = apkAsset.name,
                tamanhoBytes = apkAsset.size,
                urlSha256 = sha256Asset?.browserDownloadUrl,
            ),
        )
    }

    private fun selecionarAssetApk(assets: List<GitHubAssetDto>): GitHubAssetDto? {
        val candidatos = assets.filter { ehUrlAtualizacaoPermitida(it.browserDownloadUrl) }
        return candidatos.firstOrNull {
            it.name.startsWith("Pingo-", ignoreCase = true) &&
                it.name.endsWith("-release.apk", ignoreCase = true)
        } ?: candidatos.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true)
        }
    }

    private fun selecionarAssetSha256(assets: List<GitHubAssetDto>, nomeApk: String): GitHubAssetDto? = assets.firstOrNull {
        it.name.equals("$nomeApk.sha256", ignoreCase = true)
    } ?: assets.firstOrNull {
        it.name.startsWith("Pingo-", ignoreCase = true) &&
            it.name.endsWith("-release.apk.sha256", ignoreCase = true)
    } ?: assets.firstOrNull {
        it.name.endsWith(".sha256", ignoreCase = true)
    }

    companion object {
        private const val HEADER_ACCEPT_GITHUB = "application/vnd.github+json"
        private const val HEADER_USER_AGENT = "Pingo-Android"
    }
}

@Serializable
private data class GitHubReleaseDto(
    @SerialName("tag_name")
    val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url")
    val htmlUrl: String = "",
    val draft: Boolean = false,
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
private data class GitHubAssetDto(
    val name: String,
    val size: Long? = null,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
)

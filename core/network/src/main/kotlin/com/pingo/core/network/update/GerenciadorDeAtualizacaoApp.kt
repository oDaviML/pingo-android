package com.pingo.core.network.update

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

sealed interface EstadoAtualizacaoDoApp {
    data object Ocioso : EstadoAtualizacaoDoApp

    data object Verificando : EstadoAtualizacaoDoApp

    data class Atualizado(val versao: String) : EstadoAtualizacaoDoApp

    data class Disponivel(val info: AtualizacaoDisponivel) : EstadoAtualizacaoDoApp

    data class Baixando(val info: AtualizacaoDisponivel, val percentual: Int) : EstadoAtualizacaoDoApp

    data class ProntoParaInstalar(val info: AtualizacaoDisponivel, val arquivoApk: File, val precisaPermissaoFontes: Boolean) :
        EstadoAtualizacaoDoApp

    data class Falha(val mensagem: String, val info: AtualizacaoDisponivel? = null) : EstadoAtualizacaoDoApp
}

interface ControladorDeAtualizacaoApp {
    val versaoInstalada: String
    val codigoVersaoInstalado: Long
    val estado: StateFlow<EstadoAtualizacaoDoApp>

    suspend fun verificarEmSegundoPlano()

    suspend fun verificarManualmente()

    suspend fun baixar(info: AtualizacaoDisponivel)

    fun atualizarPermissaoInstalacao()

    fun intentPermissaoFontesDesconhecidas(): Intent

    fun intentInstalacaoApk(arquivoApk: File): Intent
}

@Singleton
@Suppress("LongParameterList")
class GerenciadorDeAtualizacaoApp internal constructor(
    private val verificador: VerificadorDeAtualizacaoApk,
    private val baixarApk: (AtualizacaoDisponivel) -> kotlinx.coroutines.flow.Flow<EstadoDownloadApk>,
    private val podeInstalar: () -> Boolean,
    private val abrirPermissaoFontes: () -> Intent,
    private val abrirInstalacaoApk: (File) -> Intent,
    override val versaoInstalada: String,
    override val codigoVersaoInstalado: Long,
    private val clock: Clock,
) : ControladorDeAtualizacaoApp {
    @Inject
    constructor(
        verificador: VerificadorDeAtualizacaoApk,
        baixador: BaixadorDeApk,
        instalador: InstaladorDeApk,
        @ApplicationContext context: Context,
    ) : this(
        verificador = verificador,
        baixarApk = baixador::baixar,
        podeInstalar = instalador::podeInstalarPacotes,
        abrirPermissaoFontes = instalador::intentPermissaoFontesDesconhecidas,
        abrirInstalacaoApk = instalador::intentInstalacao,
        versaoInstalada = lerVersaoNome(context),
        codigoVersaoInstalado = lerVersaoCodigo(context),
        clock = Clock.systemUTC(),
    )

    private val mutexVerificacao = Mutex()
    private val mutexDownload = Mutex()
    private val mutableEstado = MutableStateFlow<EstadoAtualizacaoDoApp>(EstadoAtualizacaoDoApp.Ocioso)
    private var ultimaConsultaEmMillis: Long? = null

    override val estado: StateFlow<EstadoAtualizacaoDoApp> = mutableEstado.asStateFlow()

    override suspend fun verificarEmSegundoPlano() {
        val atual = mutableEstado.value
        if (deveIgnorarConsultaEmSegundoPlano(atual)) {
            return
        }
        mutexVerificacao.withLock {
            if (deveIgnorarConsultaEmSegundoPlano(mutableEstado.value)) {
                return
            }
            val resultado = executarVerificacao()
            ultimaConsultaEmMillis = clock.millis()
            when (resultado) {
                is ResultadoVerificacaoAtualizacao.NovaVersaoDisponivel ->
                    mutableEstado.value = EstadoAtualizacaoDoApp.Disponivel(resultado.info)
                is ResultadoVerificacaoAtualizacao.Atualizado ->
                    mutableEstado.value = EstadoAtualizacaoDoApp.Atualizado(resultado.versaoAtual)
                ResultadoVerificacaoAtualizacao.SemRede, ResultadoVerificacaoAtualizacao.Indisponivel -> Unit
            }
        }
    }

    override suspend fun verificarManualmente() {
        val atual = mutableEstado.value
        if (atual is EstadoAtualizacaoDoApp.Verificando || atual is EstadoAtualizacaoDoApp.Baixando) {
            return
        }
        mutexVerificacao.withLock {
            val corrente = mutableEstado.value
            if (corrente is EstadoAtualizacaoDoApp.Verificando || corrente is EstadoAtualizacaoDoApp.Baixando) {
                return
            }
            mutableEstado.value = EstadoAtualizacaoDoApp.Verificando
            val resultado = executarVerificacao()
            ultimaConsultaEmMillis = clock.millis()
            mutableEstado.value = mapearResultadoManual(resultado)
        }
    }

    override suspend fun baixar(info: AtualizacaoDisponivel) {
        if (mutableEstado.value is EstadoAtualizacaoDoApp.Baixando) {
            return
        }
        mutexDownload.withLock {
            if (mutableEstado.value is EstadoAtualizacaoDoApp.Baixando) {
                return
            }
            baixarApk(info).collect { progresso ->
                mutableEstado.value = mapearDownload(info, progresso, podeInstalar())
            }
        }
    }

    override fun atualizarPermissaoInstalacao() {
        val atual = mutableEstado.value
        if (atual is EstadoAtualizacaoDoApp.ProntoParaInstalar) {
            mutableEstado.value = atual.copy(precisaPermissaoFontes = !podeInstalar())
        }
    }

    override fun intentPermissaoFontesDesconhecidas(): Intent = abrirPermissaoFontes()

    override fun intentInstalacaoApk(arquivoApk: File): Intent = abrirInstalacaoApk(arquivoApk)

    private fun deveIgnorarConsultaEmSegundoPlano(atual: EstadoAtualizacaoDoApp): Boolean {
        val ocupado = atual is EstadoAtualizacaoDoApp.Verificando ||
            atual is EstadoAtualizacaoDoApp.Baixando ||
            atual is EstadoAtualizacaoDoApp.ProntoParaInstalar
        if (ocupado) {
            return true
        }
        val ultima = ultimaConsultaEmMillis ?: return false
        val dentroDoIntervalo = clock.millis() - ultima < INTERVALO_VERIFICACAO_AUTO_MS
        return dentroDoIntervalo && (atual is EstadoAtualizacaoDoApp.Disponivel || atual is EstadoAtualizacaoDoApp.Atualizado)
    }

    private suspend fun executarVerificacao(): ResultadoVerificacaoAtualizacao = runCatching {
        verificador.verificar(versaoInstalada)
    }.getOrElse {
        ResultadoVerificacaoAtualizacao.Indisponivel
    }

    private fun mapearResultadoManual(resultado: ResultadoVerificacaoAtualizacao): EstadoAtualizacaoDoApp = when (resultado) {
        is ResultadoVerificacaoAtualizacao.Atualizado -> EstadoAtualizacaoDoApp.Atualizado(resultado.versaoAtual)
        is ResultadoVerificacaoAtualizacao.NovaVersaoDisponivel -> EstadoAtualizacaoDoApp.Disponivel(resultado.info)
        ResultadoVerificacaoAtualizacao.SemRede ->
            EstadoAtualizacaoDoApp.Falha("Sem conexão com a internet para verificar atualizações.")
        ResultadoVerificacaoAtualizacao.Indisponivel ->
            EstadoAtualizacaoDoApp.Falha("Não foi possível consultar atualizações agora.")
    }

    private fun mapearDownload(info: AtualizacaoDisponivel, estado: EstadoDownloadApk, podeInstalar: Boolean): EstadoAtualizacaoDoApp =
        when (estado) {
            is EstadoDownloadApk.EmProgresso -> EstadoAtualizacaoDoApp.Baixando(info, estado.percentual)
            is EstadoDownloadApk.Concluido -> EstadoAtualizacaoDoApp.ProntoParaInstalar(
                info = info,
                arquivoApk = estado.arquivoApk,
                precisaPermissaoFontes = !podeInstalar,
            )
            is EstadoDownloadApk.Falha -> EstadoAtualizacaoDoApp.Falha(mensagemFalha(estado.motivo), info)
        }

    private fun mensagemFalha(motivo: MotivoFalhaDownloadApk): String = when (motivo) {
        MotivoFalhaDownloadApk.INTEGRIDADE_INVALIDA ->
            "O arquivo baixado falhou na verificação SHA-256 e foi descartado."
        MotivoFalhaDownloadApk.SEM_REDE ->
            "Conexão perdida durante o download da atualização."
        MotivoFalhaDownloadApk.ERRO_SERVIDOR ->
            "O servidor de releases não respondeu como esperado."
        MotivoFalhaDownloadApk.ERRO_DISCO ->
            "Sem espaço disponível no aparelho para salvar o APK."
        MotivoFalhaDownloadApk.URL_INVALIDA ->
            "Endereço do pacote de atualização inválido."
    }

    private companion object {
        val INTERVALO_VERIFICACAO_AUTO_MS: Long = Duration.ofMinutes(10).toMillis()

        fun lerVersaoNome(context: Context): String = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "0.1.0" }

        fun lerVersaoCodigo(context: Context): Long = runCatching {
            PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
        }.getOrDefault(1L)
    }
}

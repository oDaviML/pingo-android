package com.pingo.feature.settings.impl

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.PackageInfoCompat
import com.pingo.core.analytics.AnalyticsFacade
import com.pingo.core.auth.AutenticadorDoSistema
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.DisponibilidadeAutenticacao
import com.pingo.core.auth.GerenciadorDeBloqueioDoApp
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.ResultadoAutenticacao
import com.pingo.core.auth.SessionState
import com.pingo.core.common.AppDispatchers
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.network.update.AtualizacaoDisponivel
import com.pingo.core.network.update.BaixadorDeApk
import com.pingo.core.network.update.EstadoDownloadApk
import com.pingo.core.network.update.InstaladorDeApk
import com.pingo.core.network.update.ResultadoVerificacaoAtualizacao
import com.pingo.core.network.update.VerificadorDeAtualizacaoApk
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.io.File
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface InformacoesVersaoApp {
    val versaoNome: String
    val versaoCodigo: Long
}

@Singleton
class InformacoesVersaoAppAndroid @Inject constructor(@param:ApplicationContext private val context: Context) : InformacoesVersaoApp {
    override val versaoNome: String by lazy {
        runCatching { packageInfo().versionName }.getOrNull().orEmpty().ifBlank { "0.1.0" }
    }

    override val versaoCodigo: Long by lazy {
        runCatching { PackageInfoCompat.getLongVersionCode(packageInfo()) }.getOrDefault(1L)
    }

    private fun packageInfo() = context.packageManager.getPackageInfo(context.packageName, 0)
}

interface DownloaderAtualizacaoApk {
    fun baixar(atualizacao: AtualizacaoDisponivel): Flow<EstadoDownloadApk>
}

@Singleton
class DownloaderAtualizacaoApkAndroid @Inject constructor(private val baixador: BaixadorDeApk) : DownloaderAtualizacaoApk {
    override fun baixar(atualizacao: AtualizacaoDisponivel): Flow<EstadoDownloadApk> = baixador.baixar(atualizacao)
}

interface GerenciadorInstalacaoApk {
    fun podeInstalarPacotes(): Boolean

    fun intentPermissaoFontesDesconhecidas(): Intent

    fun intentInstalacao(arquivoApk: File): Intent
}

@Singleton
class GerenciadorInstalacaoApkAndroid @Inject constructor(private val instalador: InstaladorDeApk) : GerenciadorInstalacaoApk {
    override fun podeInstalarPacotes(): Boolean = instalador.podeInstalarPacotes()

    override fun intentPermissaoFontesDesconhecidas(): Intent = instalador.intentPermissaoFontesDesconhecidas()

    override fun intentInstalacao(arquivoApk: File): Intent = instalador.intentInstalacao(arquivoApk)
}

@Singleton
class AtualizacaoSettingsDeps @Inject constructor(
    val verificadorDeAtualizacao: VerificadorDeAtualizacaoApk,
    val downloaderDeApk: DownloaderAtualizacaoApk,
    val gerenciadorDeInstalacao: GerenciadorInstalacaoApk,
    val informacoesVersaoApp: InformacoesVersaoApp,
)

@Singleton
class DependenciasExtrasSettings @Inject constructor(
    val authRepository: AuthRepository,
    val autenticador: AutenticadorDoSistema,
    val gerenciadorDeBloqueio: GerenciadorDeBloqueioDoApp,
    val analytics: AnalyticsFacade,
    val atualizacao: AtualizacaoSettingsDeps,
)

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    @Singleton
    abstract fun bindInformacoesVersaoApp(implementation: InformacoesVersaoAppAndroid): InformacoesVersaoApp

    @Binds
    @Singleton
    abstract fun bindDownloaderAtualizacaoApk(implementation: DownloaderAtualizacaoApkAndroid): DownloaderAtualizacaoApk

    @Binds
    @Singleton
    abstract fun bindGerenciadorInstalacaoApk(implementation: GerenciadorInstalacaoApkAndroid): GerenciadorInstalacaoApk
}

internal object SettingsSemExtras {
    fun criar(preferences: UserPreferencesDataSource): DependenciasExtrasSettings {
        val auth = SemAuthRepository
        val bloqueio = GerenciadorDeBloqueioDoApp(
            preferences = preferences,
            authRepository = auth,
            clock = Clock.systemUTC(),
            dispatchers = DispatchersSemBloqueio,
        )
        return DependenciasExtrasSettings(
            authRepository = auth,
            autenticador = SemAutenticador,
            gerenciadorDeBloqueio = bloqueio,
            analytics = SemAnalytics,
            atualizacao = AtualizacaoSettingsDeps(
                verificadorDeAtualizacao = SemVerificador,
                downloaderDeApk = SemDownloader,
                gerenciadorDeInstalacao = SemInstalador,
                informacoesVersaoApp = SemVersao,
            ),
        )
    }

    private object DispatchersSemBloqueio : AppDispatchers {
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
    }

    object SemAuthRepository : AuthRepository {
        override val session: StateFlow<SessionState> = MutableStateFlow(SessionState.LoggedOut)

        override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Cancelled

        override suspend fun logout() = Unit

        override suspend fun accessToken(forceRefresh: Boolean): String? = null
    }

    object SemAutenticador : AutenticadorDoSistema {
        override fun disponibilidade(): DisponibilidadeAutenticacao = DisponibilidadeAutenticacao.DISPONIVEL

        override suspend fun autenticar(
            activity: Activity,
            titulo: String,
            subtitulo: String?,
            descricao: String?,
        ): ResultadoAutenticacao = ResultadoAutenticacao.Cancelado

        override fun abrirConfiguracoesDeSeguranca(activity: Activity): Boolean = false
    }

    object SemAnalytics : AnalyticsFacade {
        override fun setAnalyticsEnabled(enabled: Boolean) = Unit

        override fun logEvent(name: String, attributes: Map<String, String>) = Unit

        override fun recordError(throwable: Throwable) = Unit
    }

    object SemVerificador : VerificadorDeAtualizacaoApk {
        override suspend fun verificar(versaoInstalada: String): ResultadoVerificacaoAtualizacao =
            ResultadoVerificacaoAtualizacao.Atualizado(versaoInstalada)
    }

    object SemDownloader : DownloaderAtualizacaoApk {
        override fun baixar(atualizacao: AtualizacaoDisponivel): Flow<EstadoDownloadApk> = emptyFlow()
    }

    object SemInstalador : GerenciadorInstalacaoApk {
        override fun podeInstalarPacotes(): Boolean = true

        override fun intentPermissaoFontesDesconhecidas(): Intent = Intent()

        override fun intentInstalacao(arquivoApk: File): Intent = Intent()
    }

    object SemVersao : InformacoesVersaoApp {
        override val versaoNome: String = "0.2.0"
        override val versaoCodigo: Long = 1L
    }
}

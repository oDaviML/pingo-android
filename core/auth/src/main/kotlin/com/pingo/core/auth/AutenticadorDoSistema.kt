package com.pingo.core.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

enum class DisponibilidadeAutenticacao {
    DISPONIVEL,
    SEM_CADASTRO_NO_APARELHO,
    INDISPONIVEL,
}

sealed interface ResultadoAutenticacao {
    data object Autenticado : ResultadoAutenticacao

    data object Cancelado : ResultadoAutenticacao

    data class Falha(val codigo: Int, val mensagem: String) : ResultadoAutenticacao

    data object SemCadastroNoAparelho : ResultadoAutenticacao

    data object Indisponivel : ResultadoAutenticacao
}

interface AutenticadorDoSistema {
    fun disponibilidade(): DisponibilidadeAutenticacao

    suspend fun autenticar(activity: Activity, titulo: String, subtitulo: String? = null, descricao: String? = null): ResultadoAutenticacao

    fun abrirConfiguracoesDeSeguranca(activity: Activity): Boolean
}

@Singleton
class AutenticadorBiometricoAndroid @Inject constructor(@param:ApplicationContext private val context: Context) : AutenticadorDoSistema {
    override fun disponibilidade(): DisponibilidadeAutenticacao = when (BiometricManager.from(context).canAuthenticate(AUTENTICADORES)) {
        BiometricManager.BIOMETRIC_SUCCESS -> DisponibilidadeAutenticacao.DISPONIVEL
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> DisponibilidadeAutenticacao.SEM_CADASTRO_NO_APARELHO
        else -> DisponibilidadeAutenticacao.INDISPONIVEL
    }

    override suspend fun autenticar(activity: Activity, titulo: String, subtitulo: String?, descricao: String?): ResultadoAutenticacao =
        when (disponibilidade()) {
            DisponibilidadeAutenticacao.SEM_CADASTRO_NO_APARELHO -> ResultadoAutenticacao.SemCadastroNoAparelho
            DisponibilidadeAutenticacao.INDISPONIVEL -> ResultadoAutenticacao.Indisponivel
            DisponibilidadeAutenticacao.DISPONIVEL -> {
                val fragmentActivity = activity as? FragmentActivity
                if (fragmentActivity == null) {
                    ResultadoAutenticacao.Indisponivel
                } else {
                    executarPrompt(fragmentActivity, titulo, subtitulo, descricao)
                }
            }
        }

    override fun abrirConfiguracoesDeSeguranca(activity: Activity): Boolean {
        val intentCadastro = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, AUTENTICADORES_PARA_CADASTRO)
            }
        } else {
            null
        }
        val intentAlvo = intentCadastro?.takeIf { it.resolveActivity(activity.packageManager) != null }
            ?: Intent(Settings.ACTION_SECURITY_SETTINGS)

        return runCatching {
            activity.startActivity(intentAlvo)
            true
        }.getOrDefault(false)
    }

    private suspend fun executarPrompt(
        activity: FragmentActivity,
        titulo: String,
        subtitulo: String?,
        descricao: String?,
    ): ResultadoAutenticacao = suspendCancellableCoroutine { continuation ->
        val concluido = AtomicBoolean(false)
        val callback = criarCallback(concluido) { resultado ->
            if (continuation.isActive) {
                continuation.resume(resultado)
            }
        }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .apply {
                subtitulo?.let(::setSubtitle)
                descricao?.let(::setDescription)
            }
            .setAllowedAuthenticators(AUTENTICADORES)
            .setConfirmationRequired(false)
            .build()

        continuation.invokeOnCancellation {
            runCatching { prompt.cancelAuthentication() }
        }
        prompt.authenticate(info)
    }

    private fun criarCallback(
        concluido: AtomicBoolean,
        aoConcluir: (ResultadoAutenticacao) -> Unit,
    ): BiometricPrompt.AuthenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            if (concluido.compareAndSet(false, true)) {
                aoConcluir(ResultadoAutenticacao.Autenticado)
            }
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (concluido.compareAndSet(false, true)) {
                aoConcluir(mapearErro(errorCode, errString))
            }
        }
    }

    private fun mapearErro(errorCode: Int, errString: CharSequence): ResultadoAutenticacao = when (errorCode) {
        BiometricPrompt.ERROR_USER_CANCELED,
        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
        BiometricPrompt.ERROR_CANCELED,
        -> ResultadoAutenticacao.Cancelado

        BiometricPrompt.ERROR_NO_BIOMETRICS,
        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
        -> ResultadoAutenticacao.SemCadastroNoAparelho

        else -> ResultadoAutenticacao.Falha(errorCode, errString.toString())
    }

    private companion object {
        const val AUTENTICADORES =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        const val AUTENTICADORES_PARA_CADASTRO =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}

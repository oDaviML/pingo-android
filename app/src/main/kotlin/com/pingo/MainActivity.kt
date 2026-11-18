package com.pingo

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pingo.core.auth.AutenticadorDoSistema
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.EstadoBloqueio
import com.pingo.core.auth.GerenciadorDeBloqueioDoApp
import com.pingo.core.auth.ResultadoAutenticacao
import com.pingo.core.common.AppDispatchers
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.GatilhoDeCapturaParada
import com.pingo.navigation.PingoNavHost
import com.pingo.ui.BloqueioDoAppScreen
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: AppViewModel by viewModels()

    @Inject lateinit var capturaController: Lazy<CapturaController>

    @Inject lateinit var gatilhoDeCapturaParada: Lazy<GatilhoDeCapturaParada>

    @Inject lateinit var gerenciadorDeBloqueio: GerenciadorDeBloqueioDoApp

    @Inject lateinit var autenticador: Lazy<AutenticadorDoSistema>

    @Inject lateinit var authRepository: Lazy<AuthRepository>

    @Inject lateinit var dispatchers: AppDispatchers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observarCicloDeVida()
        agendarReconciliacaoAposPrimeiroQuadro()

        setContent {
            PingoTheme {
                ConteudoPrincipal()
            }
        }
    }

    @Composable
    private fun ConteudoPrincipal() {
        val session by viewModel.session.collectAsStateWithLifecycle()
        val destinoInicial by viewModel.destinoInicial.collectAsStateWithLifecycle()
        val estadoBloqueio by gerenciadorDeBloqueio.estado.collectAsStateWithLifecycle()
        val esconderEmRecentes by gerenciadorDeBloqueio.esconderEmRecentes.collectAsStateWithLifecycle()

        LaunchedEffect(esconderEmRecentes) {
            if (esconderEmRecentes) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PingoTheme.colors.background),
        ) {
            if (estadoBloqueio != EstadoBloqueio.Carregando) {
                PingoNavHost(
                    session = session,
                    destinoInicial = destinoInicial,
                    onConsentAccepted = viewModel::onConsentAccepted,
                )
            }
            if (estadoBloqueio == EstadoBloqueio.Bloqueado) {
                CamadaDeBloqueio()
            }
        }
    }

    @Composable
    private fun CamadaDeBloqueio() {
        val escopo = rememberCoroutineScope()
        var autenticando by remember { mutableStateOf(false) }
        var mensagemErro by remember { mutableStateOf<String?>(null) }

        val tentarDesbloquear: () -> Unit = {
            if (!autenticando) {
                escopo.launch {
                    autenticando = true
                    mensagemErro = null
                    gerenciadorDeBloqueio.marcarAutenticacaoEmAndamento(true)
                    val resultado = try {
                        autenticador.get().autenticar(
                            activity = this@MainActivity,
                            titulo = "Desbloquear o Pingo",
                            subtitulo = "Confirme sua biometria ou senha do aparelho",
                        )
                    } finally {
                        gerenciadorDeBloqueio.marcarAutenticacaoEmAndamento(false)
                        autenticando = false
                    }
                    when (resultado) {
                        ResultadoAutenticacao.Autenticado -> gerenciadorDeBloqueio.marcarDesbloqueado()
                        ResultadoAutenticacao.Cancelado -> Unit
                        else -> mensagemErro = "Não foi possível desbloquear. Tente novamente."
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            tentarDesbloquear()
        }

        BloqueioDoAppScreen(
            mensagemErro = mensagemErro,
            autenticando = autenticando,
            onDesbloquear = tentarDesbloquear,
            onSairDaConta = { escopo.launch { authRepository.get().logout() } },
            onFecharApp = { finish() },
        )
    }

    private fun observarCicloDeVida() {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    gerenciadorDeBloqueio.aoVoltarParaPrimeiroPlano()
                }

                override fun onStop(owner: LifecycleOwner) {
                    if (!isChangingConfigurations) {
                        gerenciadorDeBloqueio.aoIrParaSegundoPlano()
                    }
                }
            },
        )
    }

    private fun agendarReconciliacaoAposPrimeiroQuadro() {
        window.decorView.post {
            lifecycleScope.launch(dispatchers.io) {
                capturaController.get().reconciliar()
                gatilhoDeCapturaParada.get().verificar()
            }
        }
    }
}

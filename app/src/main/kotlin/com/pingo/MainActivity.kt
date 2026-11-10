package com.pingo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.notifications.CapturaController
import com.pingo.navigation.PingoNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    @Inject lateinit var capturaController: CapturaController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch { capturaController.reconciliar() }

        setContent {
            PingoTheme {
                val session by viewModel.session.collectAsStateWithLifecycle()
                val destinoInicial by viewModel.destinoInicial.collectAsStateWithLifecycle()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PingoTheme.colors.background),
                ) {
                    PingoNavHost(
                        session = session,
                        destinoInicial = destinoInicial,
                        onConsentAccepted = viewModel::onConsentAccepted,
                    )
                }
            }
        }
    }
}

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
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.navigation.PingoNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PingoTheme {
                val session by viewModel.session.collectAsStateWithLifecycle()
                val canEnterHome by viewModel.canEnterHome.collectAsStateWithLifecycle()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PingoTheme.colors.background),
                ) {
                    PingoNavHost(
                        session = session,
                        canEnterHome = canEnterHome,
                        onConsentAccepted = viewModel::onConsentAccepted,
                    )
                }
            }
        }
    }
}

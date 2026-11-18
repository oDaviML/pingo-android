package com.pingo

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PingoApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: Lazy<HiltWorkerFactory>

    @Inject
    lateinit var inicializacaoDoApp: Lazy<InicializacaoDoApp>

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory.get())
            .build()

    override fun onCreate() {
        super.onCreate()

        Handler(Looper.getMainLooper()).postDelayed(
            { inicializacaoDoApp.get().iniciar() },
            ATRASO_INICIALIZACAO_MS,
        )
    }

    private companion object {
        const val ATRASO_INICIALIZACAO_MS = 2_500L
    }
}

package com.pingo.core.notifications

import com.pingo.core.datastore.CapturaDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface SaudeDaCaptura {
    fun observar(): Flow<DiagnosticoDaCaptura>

    suspend fun atual(): DiagnosticoDaCaptura
}

@Singleton
class MonitorDaCaptura @Inject constructor(private val capturaDataSource: CapturaDataSource, private val clock: Clock) : SaudeDaCaptura {
    override fun observar(): Flow<DiagnosticoDaCaptura> = combine(
        capturaDataSource.estadoListener,
        capturaDataSource.ultimaLeituraPorApp,
        capturaDataSource.observandoDesde,
    ) { estado, leituras, desde ->
        AvaliadorDaCaptura.avaliar(estado, leituras, desde, clock.instant())
    }

    override suspend fun atual(): DiagnosticoDaCaptura = AvaliadorDaCaptura.avaliar(
        estado = capturaDataSource.estadoListener.first(),
        leituras = capturaDataSource.ultimaLeituraPorApp.first(),
        observandoDesde = capturaDataSource.observandoDesde.first(),
        agora = clock.instant(),
    )
}

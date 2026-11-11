package com.pingo

import com.pingo.core.common.AppDispatchers
import com.pingo.core.notifications.IniciadorDeObservacaoDePendencias
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class InicializacaoDoApp @Inject constructor(
    private val observador: Provider<IniciadorDeObservacaoDePendencias>,
    private val agendadorDeSync: AgendadorDeSync,
    private val dispatchers: AppDispatchers,
) {
    private val escopo = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val iniciado = AtomicBoolean(false)

    fun iniciar() {
        if (!iniciado.compareAndSet(false, true)) {
            return
        }

        escopo.launch {
            observador.get().iniciar()
            agendadorDeSync.agendar()
        }
    }
}

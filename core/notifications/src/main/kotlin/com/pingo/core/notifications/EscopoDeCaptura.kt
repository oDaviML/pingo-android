package com.pingo.core.notifications

import com.pingo.core.common.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EscopoDeCaptura @Inject constructor(dispatchers: AppDispatchers) {
    private val escopo = CoroutineScope(SupervisorJob() + dispatchers.io)

    fun lancar(bloco: suspend () -> Unit) {
        escopo.launch { runCatching { bloco() } }
    }
}

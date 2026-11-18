package com.pingo.sync

import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncEngineExtras @Inject constructor(val clock: Clock, val agendadorDeAvisos: AgendadorDeAvisosOpcional) {
    constructor(clock: Clock = Clock.systemDefaultZone()) : this(clock, AgendadorDeAvisosOpcional())
}

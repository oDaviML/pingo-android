package com.pingo.core.data.impl

import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.reconciliacao.ReconciliadorVoucherCaptura
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextoDeCaptura @Inject constructor(
    val transacaoLocal: TransacaoLocal,
    val clock: Clock,
    val dispatchers: AppDispatchers,
    val reconciliadorVoucher: ReconciliadorVoucherCaptura,
)

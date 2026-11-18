package com.pingo.core.data.impl

import com.pingo.core.analytics.AnalyticsFacade
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
    val analytics: AnalyticsFacade,
) {
    constructor(
        transacaoLocal: TransacaoLocal,
        clock: Clock,
        dispatchers: AppDispatchers,
        reconciliadorVoucher: ReconciliadorVoucherCaptura,
    ) : this(
        transacaoLocal = transacaoLocal,
        clock = clock,
        dispatchers = dispatchers,
        reconciliadorVoucher = reconciliadorVoucher,
        analytics = AnalyticsSemOpcao,
    )

    private object AnalyticsSemOpcao : AnalyticsFacade {
        override fun setAnalyticsEnabled(enabled: Boolean) = Unit

        override fun logEvent(name: String, attributes: Map<String, String>) = Unit

        override fun recordError(throwable: Throwable) = Unit
    }
}

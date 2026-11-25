package com.pingo.core.data.impl

import com.pingo.core.analytics.AnalyticsFacade
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.ConfiguracaoCartaoRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.reconciliacao.ReconciliadorVoucherCaptura
import com.pingo.core.model.fatura.VigenciaDeCartao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
    val configuracaoCartaoRepository: ConfiguracaoCartaoRepository,
) {
    constructor(
        transacaoLocal: TransacaoLocal,
        clock: Clock,
        dispatchers: AppDispatchers,
        reconciliadorVoucher: ReconciliadorVoucherCaptura,
        analytics: AnalyticsFacade,
    ) : this(
        transacaoLocal = transacaoLocal,
        clock = clock,
        dispatchers = dispatchers,
        reconciliadorVoucher = reconciliadorVoucher,
        analytics = analytics,
        configuracaoCartaoRepository = SemConfiguracaoCartaoCaptura,
    )

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
        configuracaoCartaoRepository = SemConfiguracaoCartaoCaptura,
    )

    private object AnalyticsSemOpcao : AnalyticsFacade {
        override fun setAnalyticsEnabled(enabled: Boolean) = Unit

        override fun logEvent(name: String, attributes: Map<String, String>) = Unit

        override fun recordError(throwable: Throwable) = Unit
    }

    private object SemConfiguracaoCartaoCaptura : ConfiguracaoCartaoRepository {
        override fun observePorInstrumento(instrumentoPublicId: String): Flow<List<VigenciaDeCartao>> = emptyFlow()

        override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<VigenciaDeCartao> = emptyList()

        override suspend fun listarAtivas(): List<VigenciaDeCartao> = emptyList()
    }
}

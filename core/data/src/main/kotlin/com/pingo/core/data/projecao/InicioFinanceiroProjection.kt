package com.pingo.core.data.projecao

import com.pingo.core.model.organizacao.EstadoBlocoFinanceiro
import com.pingo.core.model.organizacao.FaturaAbertaInicio
import com.pingo.core.model.organizacao.SaldoVoucherInicio
import kotlinx.coroutines.flow.Flow

interface InicioFinanceiroProjection {
    fun observeFaturasAbertas(): Flow<EstadoBlocoFinanceiro<List<FaturaAbertaInicio>>>

    fun observeSaldosVoucher(): Flow<EstadoBlocoFinanceiro<List<SaldoVoucherInicio>>>
}

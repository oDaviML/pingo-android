package com.pingo.core.data.projecao

import com.pingo.core.model.fatura.FaturasDoCartao
import com.pingo.core.model.organizacao.ResumoContasFinanceiro
import com.pingo.core.model.voucher.DetalheVoucher
import kotlinx.coroutines.flow.Flow

interface ContasFinanceiroProjection {
    fun observeContas(): Flow<ResumoContasFinanceiro>

    fun observeFaturasDoCartao(instrumentoPublicId: String): Flow<FaturasDoCartao?>

    fun observeDetalheVoucher(instrumentoPublicId: String): Flow<DetalheVoucher?>
}

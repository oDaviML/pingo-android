package com.pingo.core.data

import com.pingo.core.model.voucher.RecargaProgramada
import com.pingo.core.model.voucher.ReferenciaSaldoVoucher
import kotlinx.coroutines.flow.Flow

interface VoucherRepository {
    fun observeReferenciasPorInstrumento(instrumentoPublicId: String): Flow<List<ReferenciaSaldoVoucher>>

    fun observeRecargasPorInstrumento(instrumentoPublicId: String): Flow<List<RecargaProgramada>>

    suspend fun listarReferenciasPorInstrumento(instrumentoPublicId: String): List<ReferenciaSaldoVoucher>

    suspend fun listarRecargasPorInstrumento(instrumentoPublicId: String): List<RecargaProgramada>

    suspend fun listarRecargasPendentes(): List<RecargaProgramada>
}

package com.pingo.core.data.impl

import com.pingo.core.data.VoucherRepository
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.database.dao.RecargaProgramadaVoucherDao
import com.pingo.core.database.dao.ReferenciaSaldoVoucherDao
import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.voucher.RecargaProgramada
import com.pingo.core.model.voucher.ReferenciaSaldoVoucher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoucherRepositoryImpl @Inject constructor(
    private val referenciaDao: ReferenciaSaldoVoucherDao,
    private val recargaDao: RecargaProgramadaVoucherDao,
    private val instrumentoDao: InstrumentoDao,
) : VoucherRepository {
    override fun observeReferenciasPorInstrumento(instrumentoPublicId: String): Flow<List<ReferenciaSaldoVoucher>> =
        referenciaDao.observePorInstrumento(instrumentoPublicId).map { lista ->
            lista.map { it.toModel() }
        }

    override fun observeRecargasPorInstrumento(instrumentoPublicId: String): Flow<List<RecargaProgramada>> = combine(
        recargaDao.observePorInstrumento(instrumentoPublicId),
        instrumentoDao.observeNaoExcluidos(),
    ) { recargas, instrumentos ->
        val beneficiosPorInstrumento = instrumentos.associate { it.publicId to (it.beneficio ?: BeneficioVoucher.VA) }
        recargas.map { recarga -> recarga.toModel(beneficiosPorInstrumento[recarga.instrumentoPublicId] ?: BeneficioVoucher.VA) }
    }

    override suspend fun listarReferenciasPorInstrumento(instrumentoPublicId: String): List<ReferenciaSaldoVoucher> =
        referenciaDao.listarPorInstrumento(instrumentoPublicId).map { it.toModel() }

    override suspend fun listarRecargasPorInstrumento(instrumentoPublicId: String): List<RecargaProgramada> {
        val beneficioPadrao = instrumentoDao.buscar(instrumentoPublicId)?.beneficio ?: BeneficioVoucher.VA
        return recargaDao.listarPorInstrumento(instrumentoPublicId).map { it.toModel(beneficioPadrao) }
    }

    override suspend fun listarRecargasPendentes(): List<RecargaProgramada> = mapearComBeneficios(recargaDao.listarPendentesAtivas())

    private suspend fun mapearComBeneficios(recargas: List<RecargaProgramadaVoucherEntity>): List<RecargaProgramada> {
        if (recargas.isEmpty()) return emptyList()
        val beneficios = instrumentoDao.listarAtivosPorTipo(TipoInstrumento.VOUCHER)
            .associate { it.publicId to (it.beneficio ?: BeneficioVoucher.VA) }
        return recargas.map { recarga ->
            val padrao = beneficios[recarga.instrumentoPublicId]
                ?: instrumentoDao.buscar(recarga.instrumentoPublicId)?.beneficio
                ?: BeneficioVoucher.VA
            recarga.toModel(padrao)
        }
    }
}

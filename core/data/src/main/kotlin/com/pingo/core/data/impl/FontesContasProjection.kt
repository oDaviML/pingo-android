package com.pingo.core.data.impl

import com.pingo.core.database.dao.ConfiguracaoCartaoDao
import com.pingo.core.database.dao.ContaDao
import com.pingo.core.database.dao.FaturaDao
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.dao.RecargaProgramadaVoucherDao
import com.pingo.core.database.dao.ReferenciaSaldoVoucherDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FontesContasProjection @Inject constructor(
    val contaDao: ContaDao,
    val instrumentoDao: InstrumentoDao,
    val faturaDao: FaturaDao,
    val configuracaoCartaoDao: ConfiguracaoCartaoDao,
    val referenciaDao: ReferenciaSaldoVoucherDao,
    val recargaDao: RecargaProgramadaVoucherDao,
    val lancamentoDao: LancamentoDao,
)

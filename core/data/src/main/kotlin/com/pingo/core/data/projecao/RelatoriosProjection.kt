package com.pingo.core.data.projecao

import com.pingo.core.model.relatorio.RelatorioMensalDetalhado
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface RelatoriosProjection {
    fun observeRelatorioMensal(mes: YearMonth): Flow<RelatorioMensalDetalhado>
}

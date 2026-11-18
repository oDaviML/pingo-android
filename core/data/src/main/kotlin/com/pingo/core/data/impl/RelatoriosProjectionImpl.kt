package com.pingo.core.data.impl

import com.pingo.core.data.mapper.toModel
import com.pingo.core.data.projecao.RelatoriosProjection
import com.pingo.core.database.dao.CategoriaDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.model.relatorio.AgregadorDeRelatorioMensal
import com.pingo.core.model.relatorio.RelatorioMensalDetalhado
import com.pingo.core.model.relatorio.ResumoCategoriaRelatorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelatoriosProjectionImpl @Inject constructor(private val lancamentoDao: LancamentoDao, private val categoriaDao: CategoriaDao) :
    RelatoriosProjection {
    override fun observeRelatorioMensal(mes: YearMonth): Flow<RelatorioMensalDetalhado> {
        val (inicioJanela, _) = AgregadorDeRelatorioMensal.intervaloDoMes(mes.minusMonths(MESES_SERIE_ANTERIORES))
        val (_, fimJanela) = AgregadorDeRelatorioMensal.intervaloDoMes(mes)
        return combine(
            lancamentoDao.observeElegiveisParaRelatorioNoPeriodo(inicioJanela, fimJanela),
            categoriaDao.observeAtivas(),
        ) { entidades, categorias ->
            val nomesPorId = categorias.associate { it.publicId to it.nome }
            val relatorio = AgregadorDeRelatorioMensal.agregar(mes, entidades.map { it.toModel() })
            val detalhadas = relatorio.categoriasDespesa.map { item ->
                ResumoCategoriaRelatorio(
                    categoriaPublicId = item.categoriaPublicId,
                    nomeCategoria = item.categoriaPublicId?.let { nomesPorId[it] } ?: NOME_SEM_CATEGORIA,
                    valorCentavos = item.valorCentavos,
                    valorMesAnteriorCentavos = item.valorMesAnteriorCentavos,
                    percentualInteiro = item.percentualInteiro,
                    pontosBase = item.pontosBase,
                    variacaoMesAnterior = item.variacaoMesAnterior,
                )
            }
            RelatorioMensalDetalhado(
                relatorio = relatorio,
                categorias = detalhadas,
                mesAtual = mes,
            )
        }
    }

    private companion object {
        const val MESES_SERIE_ANTERIORES = 5L
        const val NOME_SEM_CATEGORIA = "Sem categoria"
    }
}

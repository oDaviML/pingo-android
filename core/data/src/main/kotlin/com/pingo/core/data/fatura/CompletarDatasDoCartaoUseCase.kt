package com.pingo.core.data.fatura

import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.impl.ReconciliadorDeFaturasLocal
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.ConfiguracaoCartaoDao
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.database.entity.ConfiguracaoCartaoEntity
import com.pingo.core.model.Instrumento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.diaDoMesInformado
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ResultadoCompletarDatasCartao {
    data class Sucesso(val instrumento: Instrumento) : ResultadoCompletarDatasCartao

    data object NaoEncontrado : ResultadoCompletarDatasCartao

    data object DadosInvalidos : ResultadoCompletarDatasCartao
}

@Singleton
class CompletarDatasDoCartaoUseCase @Inject constructor(
    private val transacaoLocal: TransacaoLocal,
    private val instrumentoDao: InstrumentoDao,
    private val configuracaoCartaoDao: ConfiguracaoCartaoDao,
    private val reconciliadorDeFaturas: ReconciliadorDeFaturasLocal,
    private val clock: Clock,
) {
    suspend operator fun invoke(instrumentoPublicId: String, fechamentoDia: Int, vencimentoDia: Int): ResultadoCompletarDatasCartao {
        if (!diaDoMesInformado(fechamentoDia) || !diaDoMesInformado(vencimentoDia)) {
            return ResultadoCompletarDatasCartao.DadosInvalidos
        }
        return transacaoLocal.executar {
            val entidade = instrumentoDao.buscar(instrumentoPublicId)
                ?.takeIf { it.excluidoEm == null && it.tipo == TipoInstrumento.CREDITO }
                ?: return@executar ResultadoCompletarDatasCartao.NaoEncontrado
            val agora = clock.instant()
            val atualizado = entidade.copy(
                fechamentoDia = fechamentoDia,
                vencimentoDia = vencimentoDia,
                atualizadoEm = agora,
            )
            instrumentoDao.upsert(atualizado)
            configuracaoCartaoDao.upsert(
                ConfiguracaoCartaoEntity(
                    publicId = UUID.randomUUID().toString(),
                    instrumentoPublicId = instrumentoPublicId,
                    vigenciaInicio = agora,
                    fechamentoDia = fechamentoDia,
                    vencimentoDia = vencimentoDia,
                    criadoEm = agora,
                    atualizadoEm = agora,
                    excluidoEm = null,
                ),
            )
            reconciliadorDeFaturas.reconciliarCartaoCompleto(instrumentoPublicId)
            ResultadoCompletarDatasCartao.Sucesso(atualizado.toModel())
        }
    }
}

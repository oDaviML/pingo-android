package com.pingo.core.data.impl

import com.pingo.core.data.reconciliacao.CoordenadorDeContaFinanceiro
import com.pingo.core.database.dao.ConfiguracaoCartaoDao
import com.pingo.core.database.dao.ReferenciaSaldoVoucherDao
import com.pingo.core.database.entity.ConfiguracaoCartaoEntity
import com.pingo.core.database.entity.ReferenciaSaldoVoucherEntity
import com.pingo.core.model.Instrumento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.diaDoMesInformado
import com.pingo.core.model.voucher.TipoReferenciaSaldoVoucher
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoordenadorDeContaFinanceiroImpl @Inject constructor(
    private val configuracaoCartaoDao: ConfiguracaoCartaoDao,
    private val referenciaDao: ReferenciaSaldoVoucherDao,
) : CoordenadorDeContaFinanceiro {
    override suspend fun aoCriarInstrumentos(instrumentos: List<Instrumento>, agora: Instant) {
        instrumentos.forEach { instrumento ->
            when (instrumento.tipo) {
                TipoInstrumento.CREDITO -> registrarConfiguracaoInicial(instrumento, agora)
                TipoInstrumento.VOUCHER -> registrarAberturaVoucherSeAusente(instrumento, agora)
                TipoInstrumento.DEBITO -> Unit
            }
        }
    }

    override suspend fun aoSalvarInstrumentos(alteracoes: List<Pair<Instrumento?, Instrumento>>, agora: Instant) {
        alteracoes.forEach { (anterior, atual) ->
            when (atual.tipo) {
                TipoInstrumento.CREDITO -> if (anterior == null) {
                    registrarConfiguracaoInicial(atual, agora)
                } else {
                    registrarNovaVigenciaSeAlterada(anterior, atual, agora)
                }
                TipoInstrumento.VOUCHER -> if (anterior == null) {
                    registrarAberturaVoucherSeAusente(atual, agora)
                }
                TipoInstrumento.DEBITO -> Unit
            }
        }
    }

    private suspend fun registrarConfiguracaoInicial(instrumento: Instrumento, agora: Instant) {
        if (!datasCompletas(instrumento.fechamentoDia, instrumento.vencimentoDia)) {
            return
        }
        val existentes = configuracaoCartaoDao.listarPorInstrumento(instrumento.publicId)
        if (existentes.isNotEmpty()) {
            return
        }
        configuracaoCartaoDao.upsert(
            ConfiguracaoCartaoEntity(
                publicId = UUID.randomUUID().toString(),
                instrumentoPublicId = instrumento.publicId,
                vigenciaInicio = instrumento.criadoEm,
                fechamentoDia = instrumento.fechamentoDia,
                vencimentoDia = instrumento.vencimentoDia,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
    }

    private suspend fun registrarNovaVigenciaSeAlterada(anterior: Instrumento, atual: Instrumento, agora: Instant) {
        val alteracaoElegivel = datasCompletas(atual.fechamentoDia, atual.vencimentoDia) &&
            (anterior.fechamentoDia != atual.fechamentoDia || anterior.vencimentoDia != atual.vencimentoDia)
        if (!alteracaoElegivel) {
            return
        }
        val existentes = configuracaoCartaoDao.listarPorInstrumento(atual.publicId)
        if (existentes.isEmpty() && datasCompletas(anterior.fechamentoDia, anterior.vencimentoDia)) {
            inserirVigenciaDeDados(atual.publicId, anterior.criadoEm, anterior.fechamentoDia, anterior.vencimentoDia, agora)
            inserirVigenciaDeDados(
                atual.publicId,
                maxOf(agora, anterior.criadoEm.plusMillis(1)),
                atual.fechamentoDia,
                atual.vencimentoDia,
                agora,
            )
            return
        }
        val ultima = existentes.lastOrNull()
        if (ultima?.fechamentoDia != atual.fechamentoDia || ultima?.vencimentoDia != atual.vencimentoDia) {
            val inicio = maxOf(agora, (ultima?.vigenciaInicio ?: Instant.MIN).plusMillis(1))
            inserirVigenciaDeDados(atual.publicId, inicio, atual.fechamentoDia, atual.vencimentoDia, agora)
        }
    }

    private suspend fun inserirVigenciaDeDados(
        instrumentoPublicId: String,
        inicio: Instant,
        fechamentoDia: Int?,
        vencimentoDia: Int?,
        agora: Instant,
    ) {
        configuracaoCartaoDao.upsert(
            ConfiguracaoCartaoEntity(
                publicId = UUID.randomUUID().toString(),
                instrumentoPublicId = instrumentoPublicId,
                vigenciaInicio = inicio,
                fechamentoDia = fechamentoDia,
                vencimentoDia = vencimentoDia,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
    }

    private suspend fun registrarAberturaVoucherSeAusente(instrumento: Instrumento, agora: Instant) {
        val saldoInicial = instrumento.saldoCentavos ?: return
        val existentes = referenciaDao.listarPorInstrumento(instrumento.publicId)
        if (existentes.any { it.tipoReferencia == TipoReferenciaSaldoVoucher.ABERTURA }) {
            return
        }
        referenciaDao.upsert(
            ReferenciaSaldoVoucherEntity(
                publicId = UUID.randomUUID().toString(),
                instrumentoPublicId = instrumento.publicId,
                tipoReferencia = TipoReferenciaSaldoVoucher.ABERTURA,
                saldoCentavos = saldoInicial,
                deltaCentavos = null,
                instanteReferencia = instrumento.criadoEm,
                lancamentoPublicId = null,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
    }

    private fun datasCompletas(fechamentoDia: Int?, vencimentoDia: Int?): Boolean =
        diaDoMesInformado(fechamentoDia) && diaDoMesInformado(vencimentoDia)
}

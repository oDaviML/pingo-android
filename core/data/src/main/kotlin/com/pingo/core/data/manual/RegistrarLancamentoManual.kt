package com.pingo.core.data.manual

import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.util.UUID
import javax.inject.Inject

class RegistrarLancamentoManual @Inject constructor(
    private val lancamentoRepository: LancamentoRepository,
    private val instrumentoRepository: InstrumentoRepository,
    private val transacaoLocal: TransacaoLocal,
    private val clock: Clock,
) {
    suspend fun ultimoInstrumentoUsado(): String? {
        val ativos = instrumentosAtivos()
        if (ativos.isEmpty()) {
            return null
        }

        return lancamentoRepository.ultimoInstrumentoUsado(ativos.map { it.publicId })
            ?: ativos.singleOrNull()?.publicId
    }

    suspend fun registrar(novo: NovoLancamentoManual): ResultadoLancamentoManual {
        val ativos = instrumentosAtivos().associateBy { it.publicId }
        val invalidos = validar(novo, ativos)

        if (invalidos.isNotEmpty()) {
            return ResultadoLancamentoManual.Invalido(invalidos)
        }

        return when (novo.tipo) {
            TipoLancamento.TRANSFERENCIA -> registrarTransferencia(novo)
            TipoLancamento.DESPESA, TipoLancamento.RECEITA -> registrarLancamento(novo)
            TipoLancamento.AJUSTE -> ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.TIPO))
        }
    }

    private suspend fun instrumentosAtivos(): List<Instrumento> =
        instrumentoRepository.observeInstrumentos().first().filter { instrumento ->
            instrumento.excluidoEm == null && !instrumento.arquivado
        }

    private fun validar(novo: NovoLancamentoManual, ativos: Map<String, Instrumento>): Set<CampoDeLancamentoManual> = buildSet {
        if (novo.valorCentavos <= 0) {
            add(CampoDeLancamentoManual.VALOR)
        }

        if (novo.tipo == TipoLancamento.TRANSFERENCIA) {
            val origem = novo.instrumentoPublicId
            val destino = novo.destinoPublicId

            if (origem == null || origem !in ativos) {
                add(CampoDeLancamentoManual.ORIGEM)
            }
            if (destino == null || destino !in ativos || destino == origem) {
                add(CampoDeLancamentoManual.DESTINO)
            }
        } else if (novo.instrumentoPublicId == null || novo.instrumentoPublicId !in ativos) {
            add(CampoDeLancamentoManual.INSTRUMENTO)
        }
    }

    private suspend fun registrarLancamento(novo: NovoLancamentoManual): ResultadoLancamentoManual {
        val instrumento = requireNotNull(novo.instrumentoPublicId)
        val duplicata = temCapturadoSemelhante(instrumento, novo)
        val lancamento = montar(novo, instrumento, duplicata = duplicata, transferenciaId = null)
        lancamentoRepository.salvar(lancamento)

        return ResultadoLancamentoManual.Salvo(listOf(lancamento))
    }

    private suspend fun registrarTransferencia(novo: NovoLancamentoManual): ResultadoLancamentoManual {
        val origem = requireNotNull(novo.instrumentoPublicId)
        val destino = requireNotNull(novo.destinoPublicId)
        val transferenciaId = UUID.randomUUID().toString()
        val saida = montar(novo, origem, duplicata = temCapturadoSemelhante(origem, novo), transferenciaId = transferenciaId)
        val entrada = saida
            .copy(
                publicId = UUID.randomUUID().toString(),
                instrumentoPublicId = destino,
                tipoSemTransferencia = TipoLancamento.RECEITA,
            )
            .comDuplicata(temCapturadoSemelhante(destino, novo))

        transacaoLocal.executar {
            lancamentoRepository.salvar(saida)
            lancamentoRepository.salvar(entrada)
        }

        return ResultadoLancamentoManual.Salvo(listOf(saida, entrada))
    }

    private suspend fun temCapturadoSemelhante(instrumentoPublicId: String, novo: NovoLancamentoManual): Boolean =
        lancamentoRepository.capturadosSemelhantes(
            instrumentoPublicId = instrumentoPublicId,
            valorCentavos = novo.valorCentavos,
            inicio = novo.dataHora.minus(JANELA_DE_DUPLICATA),
            fim = novo.dataHora.plus(JANELA_DE_DUPLICATA),
        ).isNotEmpty()

    private fun montar(novo: NovoLancamentoManual, instrumentoPublicId: String, duplicata: Boolean, transferenciaId: String?): Lancamento {
        val agora = clock.instant()
        val transferencia = novo.tipo == TipoLancamento.TRANSFERENCIA

        return Lancamento(
            publicId = UUID.randomUUID().toString(),
            instrumentoPublicId = instrumentoPublicId,
            categoriaPublicId = novo.categoriaPublicId.takeUnless { transferencia },
            transferenciaId = transferenciaId,
            valorCentavos = novo.valorCentavos,
            dataHora = novo.dataHora,
            tipo = novo.tipo,
            tipoSemTransferencia = TipoLancamento.DESPESA.takeIf { transferencia },
            estabelecimento = novo.estabelecimento?.trim()?.takeIf { it.isNotEmpty() },
            status = statusDaDuplicata(duplicata),
            origem = OrigemLancamento.MANUAL,
            confianca = 1.0,
            motivoPendencia = motivoDaDuplicata(duplicata),
            criadoEm = agora,
            atualizadoEm = agora,
        )
    }

    private fun Lancamento.comDuplicata(duplicata: Boolean): Lancamento = copy(
        status = statusDaDuplicata(duplicata),
        motivoPendencia = motivoDaDuplicata(duplicata),
    )

    private fun statusDaDuplicata(duplicata: Boolean): StatusLancamento =
        if (duplicata) StatusLancamento.PENDENTE else StatusLancamento.CONFIRMADO

    private fun motivoDaDuplicata(duplicata: Boolean): String? = if (duplicata) MotivoPendencia.POSSIVEL_DUPLICATA.name else null

    private companion object {
        val JANELA_DE_DUPLICATA: Duration = Duration.ofHours(24)
    }
}

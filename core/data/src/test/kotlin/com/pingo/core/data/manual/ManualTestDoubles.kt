package com.pingo.core.data.manual

import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

internal val AGORA_MANUAL: Instant = Instant.parse("2026-11-10T15:30:00Z")
internal val FUSO_MANUAL: ZoneId = ZoneOffset.UTC

internal class AmbienteManual {
    val instrumentos = FakeInstrumentosManual()
    val lancamentos = FakeLancamentosManual()
    val transacao = FakeTransacaoLocal()
    val registrar = RegistrarLancamentoManual(
        lancamentoRepository = lancamentos,
        instrumentoRepository = instrumentos,
        transacaoLocal = transacao,
        clock = Clock.fixed(AGORA_MANUAL, FUSO_MANUAL),
    )
}

internal class FakeInstrumentosManual : InstrumentoRepository {
    private val tabela = MutableStateFlow<List<Instrumento>>(emptyList())

    fun criar(apelido: String = "Cartão", arquivado: Boolean = false, contaPublicId: String = "conta-1"): Instrumento {
        val instrumento = Instrumento(
            publicId = UUID.randomUUID().toString(),
            contaPublicId = contaPublicId,
            tipo = TipoInstrumento.CREDITO,
            apelido = apelido,
            arquivado = arquivado,
            criadoEm = AGORA_MANUAL,
            atualizadoEm = AGORA_MANUAL,
        )
        tabela.value = tabela.value + instrumento

        return instrumento
    }

    fun todos(): List<Instrumento> = tabela.value

    override fun observeInstrumentos(): Flow<List<Instrumento>> = tabela.map { lista -> lista.filter { it.excluidoEm == null } }

    override fun observePorConta(contaPublicId: String): Flow<List<Instrumento>> =
        observeInstrumentos().map { lista -> lista.filter { it.contaPublicId == contaPublicId } }

    override suspend fun buscar(publicId: String): Instrumento? = tabela.value.firstOrNull { it.publicId == publicId }

    override suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<Instrumento> =
        tabela.value.filter { it.excluidoEm == null && !it.arquivado }

    override suspend fun salvar(instrumento: Instrumento) = salvarTodos(listOf(instrumento))

    override suspend fun salvarTodos(instrumentos: List<Instrumento>) {
        tabela.value = tabela.value.filterNot { atual -> instrumentos.any { it.publicId == atual.publicId } } + instrumentos
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        tabela.value = tabela.value.map { atual ->
            if (atual.publicId == publicId) atual.copy(excluidoEm = quando, atualizadoEm = quando) else atual
        }
    }

    override suspend fun arquivar(publicId: String, quando: Instant) {
        tabela.value = tabela.value.map { atual ->
            if (atual.publicId == publicId) atual.copy(arquivado = true, atualizadoEm = quando) else atual
        }
    }

    override suspend fun reativar(publicId: String, quando: Instant) {
        tabela.value = tabela.value.map { atual ->
            if (atual.publicId == publicId) {
                atual.copy(arquivado = false, arquivadoPorCascata = false, atualizadoEm = quando)
            } else {
                atual
            }
        }
    }
}

internal class FakeLancamentosManual : LancamentoRepository {
    private val tabela = MutableStateFlow<List<Lancamento>>(emptyList())

    fun todos(): List<Lancamento> = tabela.value

    override fun observeLancamentos(): Flow<List<Lancamento>> =
        tabela.map { lista -> lista.filter { it.excluidoEm == null }.sortedByDescending { it.dataHora } }

    override fun observePorPeriodo(inicio: Instant, fim: Instant): Flow<List<Lancamento>> =
        observeLancamentos().map { lista -> lista.filter { it.dataHora in inicio..fim } }

    override suspend fun buscar(publicId: String): Lancamento? = tabela.value.firstOrNull { it.publicId == publicId }

    override suspend fun buscarPorHash(hash: String): Lancamento? =
        tabela.value.firstOrNull { it.hashDuplicata == hash && it.excluidoEm == null }

    override suspend fun candidatosDeTransferencia(
        tipo: TipoLancamento,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
        contaPublicId: String,
        origem: OrigemLancamento,
    ): List<Lancamento> = emptyList()

    override suspend fun ultimoInstrumentoUsado(instrumentos: List<String>): String? = tabela.value
        .filter { it.excluidoEm == null && it.instrumentoPublicId != null && it.instrumentoPublicId in instrumentos }
        .maxByOrNull { it.dataHora }
        ?.instrumentoPublicId

    override suspend fun capturadosSemelhantes(
        instrumentoPublicId: String,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
    ): List<Lancamento> = tabela.value.filter { lancamento ->
        lancamento.excluidoEm == null &&
            lancamento.origem == OrigemLancamento.NOTIFICACAO &&
            lancamento.instrumentoPublicId == instrumentoPublicId &&
            lancamento.valorCentavos == valorCentavos &&
            lancamento.dataHora in inicio..fim
    }

    override suspend fun salvar(lancamento: Lancamento) = salvarTodos(listOf(lancamento))

    override suspend fun salvarTodos(lancamentos: List<Lancamento>) {
        tabela.value = tabela.value.filterNot { atual -> lancamentos.any { it.publicId == atual.publicId } } + lancamentos
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        tabela.value = tabela.value.map { atual ->
            if (atual.publicId == publicId) atual.copy(excluidoEm = quando, atualizadoEm = quando) else atual
        }
    }
}

internal class FakeTransacaoLocal : TransacaoLocal {
    var execucoes: Int = 0

    override suspend fun <T> executar(bloco: suspend () -> T): T {
        execucoes++

        return bloco()
    }
}

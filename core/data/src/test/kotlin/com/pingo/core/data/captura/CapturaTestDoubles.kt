package com.pingo.core.data.captura

import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

internal val AGORA_DA_CAPTURA: Instant = Instant.parse("2026-11-10T13:54:48Z")
internal val FUSO_DA_CAPTURA: ZoneId = ZoneId.of("America/Sao_Paulo")

internal class MutableClock(var agora: Instant, private val fuso: ZoneId) : Clock() {
    override fun getZone(): ZoneId = fuso

    override fun withZone(zone: ZoneId): Clock = MutableClock(agora, zone)

    override fun instant(): Instant = agora
}

internal class FakeContas {
    private val contas = mutableListOf<Conta>()

    fun criar(instituicao: InstituicaoFinanceira, apelido: String = instituicao.name): Conta {
        val conta = Conta(
            publicId = UUID.randomUUID().toString(),
            instituicao = instituicao,
            apelido = apelido,
            tipo = TipoConta.CORRENTE,
            criadoEm = AGORA_DA_CAPTURA,
            atualizadoEm = AGORA_DA_CAPTURA,
        )
        contas += conta

        return conta
    }

    fun porId(publicId: String): Conta? = contas.firstOrNull { it.publicId == publicId }

    fun instituicaoDe(contaPublicId: String): InstituicaoFinanceira? = porId(contaPublicId)?.instituicao
}

internal class FakeInstrumentos(private val contas: FakeContas) : InstrumentoRepository {
    private val instrumentos = mutableListOf<Instrumento>()

    fun criar(
        instituicao: InstituicaoFinanceira,
        digitos: String? = null,
        apelido: String = "Cartão ${instrumentos.size + 1}",
    ): Instrumento = criarNaConta(contas.criar(instituicao), digitos, apelido)

    fun criarNaConta(conta: Conta, digitos: String? = null, apelido: String = "Cartão ${instrumentos.size + 1}"): Instrumento {
        val instrumento = Instrumento(
            publicId = UUID.randomUUID().toString(),
            contaPublicId = conta.publicId,
            tipo = TipoInstrumento.CREDITO,
            apelido = apelido,
            ultimosDigitos = digitos,
            criadoEm = AGORA_DA_CAPTURA,
            atualizadoEm = AGORA_DA_CAPTURA,
        )
        instrumentos += instrumento

        return instrumento
    }

    fun todos(): List<Instrumento> = instrumentos.toList()

    override fun observeInstrumentos(): Flow<List<Instrumento>> =
        flowOf(instrumentos.filter { instrumento -> instrumento.excluidoEm == null })

    override fun observePorConta(contaPublicId: String): Flow<List<Instrumento>> =
        observeInstrumentos().map { lista -> lista.filter { instrumento -> instrumento.contaPublicId == contaPublicId } }

    override suspend fun buscar(publicId: String): Instrumento? = instrumentos.firstOrNull { it.publicId == publicId }

    override suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<Instrumento> = instrumentos
        .filter { instrumento ->
            contas.instituicaoDe(instrumento.contaPublicId) == instituicao &&
                instrumento.excluidoEm == null &&
                !instrumento.arquivado
        }
        .sortedBy { it.apelido }

    override suspend fun salvar(instrumento: Instrumento) = salvarTodos(listOf(instrumento))

    override suspend fun salvarTodos(instrumentos: List<Instrumento>) {
        this.instrumentos.removeAll { atual -> instrumentos.any { it.publicId == atual.publicId } }
        this.instrumentos += instrumentos
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        val posicao = instrumentos.indexOfFirst { it.publicId == publicId }
        if (posicao >= 0) {
            instrumentos[posicao] = instrumentos[posicao].copy(excluidoEm = quando, atualizadoEm = quando)
        }
    }

    override suspend fun arquivar(publicId: String, quando: Instant) {
        val posicao = instrumentos.indexOfFirst { it.publicId == publicId }
        if (posicao >= 0) {
            instrumentos[posicao] = instrumentos[posicao].copy(arquivado = true, atualizadoEm = quando)
        }
    }

    override suspend fun reativar(publicId: String, quando: Instant) {
        val posicao = instrumentos.indexOfFirst { it.publicId == publicId }
        if (posicao >= 0) {
            instrumentos[posicao] = instrumentos[posicao].copy(
                arquivado = false,
                arquivadoPorCascata = false,
                atualizadoEm = quando,
            )
        }
    }
}

internal class FakeLancamentos(private val instrumentos: FakeInstrumentos) : LancamentoRepository {
    private val lancamentos = mutableListOf<Lancamento>()

    fun todos(): List<Lancamento> = lancamentos.toList()

    override fun observeLancamentos(): Flow<List<Lancamento>> =
        flowOf(lancamentos.filter { it.excluidoEm == null }.sortedByDescending { it.dataHora })

    override fun observePorPeriodo(inicio: Instant, fim: Instant): Flow<List<Lancamento>> =
        observeLancamentos().map { lista -> lista.filter { it.dataHora in inicio..fim } }

    override suspend fun buscar(publicId: String): Lancamento? = lancamentos.firstOrNull { it.publicId == publicId }

    override suspend fun buscarPorHash(hash: String): Lancamento? =
        lancamentos.firstOrNull { it.hashDuplicata == hash && it.excluidoEm == null }

    override suspend fun ultimoInstrumentoUsado(instrumentos: List<String>): String? = lancamentos
        .filter { it.excluidoEm == null && it.instrumentoPublicId in instrumentos }
        .maxByOrNull { it.dataHora }
        ?.instrumentoPublicId

    override suspend fun capturadosSemelhantes(
        instrumentoPublicId: String,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
    ): List<Lancamento> = lancamentos.filter { lancamento ->
        lancamento.excluidoEm == null &&
            lancamento.origem == OrigemLancamento.NOTIFICACAO &&
            lancamento.instrumentoPublicId == instrumentoPublicId &&
            lancamento.valorCentavos == valorCentavos &&
            lancamento.dataHora in inicio..fim
    }

    override suspend fun candidatosDeTransferencia(
        tipo: TipoLancamento,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
        contaPublicId: String,
        origem: OrigemLancamento,
    ): List<Lancamento> = lancamentos.filter { lancamento ->
        lancamento.tipo == tipo &&
            lancamento.valorCentavos == valorCentavos &&
            lancamento.dataHora in inicio..fim &&
            lancamento.origem == origem &&
            lancamento.transferenciaId == null &&
            lancamento.excluidoEm == null &&
            contaDo(lancamento).let { conta -> conta != null && conta != contaPublicId }
    }

    override suspend fun salvar(lancamento: Lancamento) = salvarTodos(listOf(lancamento))

    override suspend fun salvarTodos(lancamentos: List<Lancamento>) {
        this.lancamentos.removeAll { atual -> lancamentos.any { it.publicId == atual.publicId } }
        this.lancamentos += lancamentos
    }

    override suspend fun excluir(publicId: String, quando: Instant) {
        val posicao = lancamentos.indexOfFirst { it.publicId == publicId }
        if (posicao >= 0) {
            lancamentos[posicao] = lancamentos[posicao].copy(excluidoEm = quando, atualizadoEm = quando)
        }
    }

    private fun contaDo(lancamento: Lancamento): String? =
        instrumentos.todos().firstOrNull { it.publicId == lancamento.instrumentoPublicId }?.contaPublicId
}

package com.pingo.feature.accounts.impl

import com.pingo.core.data.CadastroSalvo
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.projecao.ContasFinanceiroProjection
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.ContaEditada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.NovaConta
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.fatura.FaturasDoCartao
import com.pingo.core.model.organizacao.ResumoContasFinanceiro
import com.pingo.core.model.voucher.DetalheVoucher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant

internal class FakeContaRepository : ContaRepository {
    val contas = MutableStateFlow<List<Conta>>(emptyList())
    val criadas = mutableListOf<List<NovaConta>>()
    val salvas = mutableListOf<ContaEditada>()
    var contaPorId: Conta? = null
    var falhaAoObservar: Throwable? = null
    var falhaAoSalvar: Throwable? = null
    var falhaAoArquivar: Throwable? = null
    var gateDeSalvamento: CompletableDeferred<Unit>? = null
    val arquivadas = mutableListOf<String>()
    val reativadas = mutableListOf<String>()

    override fun observeContas(): Flow<List<Conta>> = flow {
        falhaAoObservar?.let { throw it }
        emitAll(contas)
    }

    override suspend fun buscar(publicId: String): Conta? = contaPorId

    override suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant, publicIds: List<String>): List<CadastroSalvo> {
        gateDeSalvamento?.await()
        falhaAoSalvar?.let { throw it }
        criadas.add(contas)

        return emptyList()
    }

    override suspend fun salvarCadastro(edicao: ContaEditada, agora: Instant): CadastroSalvo {
        gateDeSalvamento?.await()
        falhaAoSalvar?.let { throw it }
        salvas.add(edicao)

        return CadastroSalvo(
            conta = Conta(
                publicId = edicao.publicId,
                instituicao = contaPorId?.instituicao ?: InstituicaoFinanceira.NUBANK,
                apelido = edicao.apelido,
                tipo = edicao.tipo,
                criadoEm = contaPorId?.criadoEm ?: agora,
                atualizadoEm = agora,
            ),
            instrumentos = emptyList(),
        )
    }

    override suspend fun salvar(conta: Conta) = Unit

    override suspend fun salvarTodas(contas: List<Conta>) = Unit

    override suspend fun excluir(publicId: String, quando: Instant) = Unit

    override suspend fun arquivar(publicId: String, quando: Instant) {
        falhaAoArquivar?.let { throw it }
        arquivadas.add(publicId)
        atualizarArquivamento(publicId, arquivada = true)
    }

    override suspend fun reativar(publicId: String, quando: Instant) {
        falhaAoArquivar?.let { throw it }
        reativadas.add(publicId)
        atualizarArquivamento(publicId, arquivada = false)
    }

    private fun atualizarArquivamento(publicId: String, arquivada: Boolean) {
        contaPorId = contaPorId?.takeIf { it.publicId == publicId }?.copy(arquivada = arquivada) ?: contaPorId
        contas.value = contas.value.map { if (it.publicId == publicId) it.copy(arquivada = arquivada) else it }
    }
}

internal class FakeInstrumentoRepository : InstrumentoRepository {
    val instrumentos = MutableStateFlow<List<Instrumento>>(emptyList())
    var porConta: List<Instrumento> = emptyList()
    var falhaAoArquivar: Throwable? = null
    val arquivados = mutableListOf<String>()
    val reativados = mutableListOf<String>()

    override fun observeInstrumentos(): Flow<List<Instrumento>> = instrumentos

    override fun observePorConta(contaPublicId: String): Flow<List<Instrumento>> =
        instrumentos.map { lista -> porConta.ifEmpty { lista.filter { it.contaPublicId == contaPublicId } } }

    override suspend fun buscar(publicId: String): Instrumento? = instrumentos.value.firstOrNull { it.publicId == publicId }

    override suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<Instrumento> =
        instrumentos.value.filter { !it.arquivado }

    override suspend fun salvar(instrumento: Instrumento) = Unit

    override suspend fun salvarTodos(instrumentos: List<Instrumento>) = Unit

    override suspend fun excluir(publicId: String, quando: Instant) = Unit

    override suspend fun arquivar(publicId: String, quando: Instant) {
        falhaAoArquivar?.let { throw it }
        arquivados.add(publicId)
        atualizar(publicId) { it.copy(arquivado = true) }
    }

    override suspend fun reativar(publicId: String, quando: Instant) {
        falhaAoArquivar?.let { throw it }
        reativados.add(publicId)
        atualizar(publicId) { it.copy(arquivado = false, arquivadoPorCascata = false) }
    }

    private fun atualizar(publicId: String, transformar: (Instrumento) -> Instrumento) {
        instrumentos.value = instrumentos.value.map { if (it.publicId == publicId) transformar(it) else it }
        porConta = porConta.map { if (it.publicId == publicId) transformar(it) else it }
    }
}

internal class FakeContasFinanceiroProjection : ContasFinanceiroProjection {
    val resumo = MutableStateFlow(ResumoContasFinanceiro(emptyList(), emptyList()))
    val faturasPorInstrumento = MutableStateFlow<Map<String, FaturasDoCartao?>>(emptyMap())
    val detalheVoucher = MutableStateFlow<DetalheVoucher?>(null)
    var falhaAoObservarContas: Throwable? = null
    var falhaAoObservarFaturas: Throwable? = null

    override fun observeContas(): Flow<ResumoContasFinanceiro> = flow {
        falhaAoObservarContas?.let { throw it }
        emitAll(resumo)
    }

    override fun observeFaturasDoCartao(instrumentoPublicId: String): Flow<FaturasDoCartao?> = flow {
        falhaAoObservarFaturas?.let { throw it }
        emitAll(faturasPorInstrumento.map { it[instrumentoPublicId] })
    }

    override fun observeDetalheVoucher(instrumentoPublicId: String): Flow<DetalheVoucher?> = detalheVoucher
}

internal class FakeCategoriaRepository : CategoriaRepository {
    val categorias = MutableStateFlow<List<Categoria>>(emptyList())

    override fun observeCategorias(): Flow<List<Categoria>> = categorias

    override fun observeEscolhiveis(): Flow<List<Categoria>> = categorias

    override suspend fun buscar(publicId: String): Categoria? = categorias.value.firstOrNull { it.publicId == publicId }

    override suspend fun criar(nome: String, tipo: TipoCategoria, agora: Instant): ResultadoCategoria = error("Não esperado em teste")

    override suspend fun renomear(publicId: String, novoNome: String, quando: Instant): ResultadoCategoria = error("Não esperado em teste")

    override suspend fun arquivar(publicId: String, quando: Instant) = Unit

    override suspend fun salvar(categoria: Categoria) = Unit

    override suspend fun salvarTodas(categorias: List<Categoria>) = Unit

    override suspend fun excluir(publicId: String, quando: Instant) = Unit
}

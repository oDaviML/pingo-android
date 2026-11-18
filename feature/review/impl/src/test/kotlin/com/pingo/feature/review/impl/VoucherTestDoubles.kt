package com.pingo.feature.review.impl

import com.pingo.core.data.mapper.toEntity
import com.pingo.core.data.mapper.toModel
import com.pingo.core.database.dao.CategoriaDao
import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.database.dao.LancamentoDao
import com.pingo.core.database.dao.RecargaProgramadaVoucherDao
import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate

class FakeLancamentoDao(private val repo: FakeLancamentoRepository) : LancamentoDao() {
    override fun observeAtivos(): Flow<List<LancamentoEntity>> = repo.observeLancamentos().map { list -> list.map { it.toEntity() } }

    override fun observePorPeriodo(inicio: Instant, fim: Instant): Flow<List<LancamentoEntity>> =
        repo.observePorPeriodo(inicio, fim).map { list -> list.map { it.toEntity() } }

    override fun observeElegiveisParaRelatorioNoPeriodo(inicioInclusivo: Instant, fimExclusivo: Instant): Flow<List<LancamentoEntity>> =
        observeAtivos()

    override suspend fun listarElegiveisParaRelatorioNoPeriodo(inicioInclusivo: Instant, fimExclusivo: Instant): List<LancamentoEntity> =
        emptyList()

    override fun observeElegiveisParaDeteccao(): Flow<List<LancamentoEntity>> = observeAtivos()

    override suspend fun listarElegiveisParaDeteccao(): List<LancamentoEntity> = emptyList()

    override suspend fun listarPorFatura(faturaPublicId: String): List<LancamentoEntity> = emptyList()

    override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<LancamentoEntity> = emptyList()

    override fun observePorAssinatura(assinaturaPublicId: String): Flow<List<LancamentoEntity>> = observeAtivos()

    override suspend fun listarPorAssinatura(assinaturaPublicId: String): List<LancamentoEntity> = emptyList()

    override suspend fun buscar(publicId: String): LancamentoEntity? = repo.buscar(publicId)?.toEntity()

    override suspend fun buscarPorHash(hash: String): LancamentoEntity? = repo.buscarPorHash(hash)?.toEntity()

    override suspend fun ultimoInstrumentoUsado(instrumentos: List<String>): String? = repo.ultimoInstrumentoUsado(instrumentos)

    override suspend fun capturadosSemelhantes(
        instrumentoPublicId: String,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
        origem: OrigemLancamento,
    ): List<LancamentoEntity> = emptyList()

    override suspend fun candidatosDeTransferencia(
        tipo: TipoLancamento,
        valorCentavos: Long,
        inicio: Instant,
        fim: Instant,
        contaPublicId: String,
        origem: OrigemLancamento,
    ): List<LancamentoEntity> = emptyList()

    override suspend fun atualizadosDesde(desde: Instant): List<LancamentoEntity> = emptyList()

    override suspend fun upsert(lancamento: LancamentoEntity) {
        repo.salvar(lancamento.toModel())
    }

    override suspend fun upsertAll(lancamentos: List<LancamentoEntity>) {
        repo.salvarTodos(lancamentos.map { it.toModel() })
    }

    override suspend fun marcarExcluido(publicId: String, quando: Instant) {
        repo.excluir(publicId, quando)
    }

    override suspend fun definirTextoBruto(publicId: String, textoBruto: String) = Unit

    override suspend fun repointarCompraPai(de: String, para: String) = Unit

    override suspend fun repointarEstorno(de: String, para: String) = Unit

    override suspend fun apagar(publicId: String) = Unit
}

class FakeInstrumentoDao(private val repo: FakeInstrumentoRepository) : InstrumentoDao {
    override fun observeNaoExcluidos(): Flow<List<InstrumentoEntity>> =
        repo.observeInstrumentos().map { list -> list.filter { it.excluidoEm == null }.map { it.toEntity() } }

    override fun observeAtivos(): Flow<List<InstrumentoEntity>> =
        repo.observeInstrumentos().map { list -> list.filter { it.excluidoEm == null && !it.arquivado }.map { it.toEntity() } }

    override suspend fun buscar(publicId: String): InstrumentoEntity? = repo.buscar(publicId)?.toEntity()

    override suspend fun buscarAtivo(publicId: String): InstrumentoEntity? =
        repo.buscar(publicId)?.takeIf { it.excluidoEm == null && !it.arquivado }?.toEntity()

    override fun observePorConta(contaPublicId: String): Flow<List<InstrumentoEntity>> =
        repo.observePorConta(contaPublicId).map { list -> list.map { it.toEntity() } }

    override suspend fun ativosDaInstituicao(instituicao: InstituicaoFinanceira): List<InstrumentoEntity> =
        repo.ativosDaInstituicao(instituicao).map { it.toEntity() }

    override suspend fun listarPorConta(contaPublicId: String): List<InstrumentoEntity> =
        repo.todos().filter { it.contaPublicId == contaPublicId && it.excluidoEm == null }.map { it.toEntity() }

    override suspend fun listarAtivosPorTipo(tipo: TipoInstrumento): List<InstrumentoEntity> =
        repo.todos().filter { it.tipo == tipo && it.excluidoEm == null && !it.arquivado }.map { it.toEntity() }

    override suspend fun atualizadosDesde(desde: Instant): List<InstrumentoEntity> = emptyList()

    override suspend fun upsert(instrumento: InstrumentoEntity) = Unit

    override suspend fun upsertAll(instrumentos: List<InstrumentoEntity>) = Unit

    override suspend fun marcarExcluido(publicId: String, quando: Instant) = Unit

    override suspend fun marcarArquivado(publicId: String, quando: Instant) = Unit

    override suspend fun marcarReativado(publicId: String, quando: Instant) = Unit

    override suspend fun arquivarAtivosPorCascata(contaPublicId: String, quando: Instant) = Unit

    override suspend fun reativarArquivadosPorCascata(contaPublicId: String, quando: Instant) = Unit
}

class FakeCategoriaDao(private val repo: FakeCategoriaRepository) : CategoriaDao {
    override fun observeAtivas(): Flow<List<CategoriaEntity>> =
        repo.observeCategorias().map { list -> list.filter { it.excluidoEm == null }.map { it.toEntity() } }

    override suspend fun listarAtivas(): List<CategoriaEntity> =
        repo.observeCategorias().first().filter { it.excluidoEm == null }.map { it.toEntity() }

    override suspend fun buscar(publicId: String): CategoriaEntity? = repo.buscar(publicId)?.toEntity()

    override suspend fun atualizadasDesde(desde: Instant): List<CategoriaEntity> = emptyList()

    override suspend fun upsert(categoria: CategoriaEntity) = Unit

    override suspend fun upsertAll(categorias: List<CategoriaEntity>) = Unit

    override suspend fun marcarExcluida(publicId: String, quando: Instant) = Unit
}

class FakeRecargaProgramadaVoucherDao(iniciais: List<RecargaProgramadaVoucherEntity> = emptyList()) : RecargaProgramadaVoucherDao {
    private val recargas = MutableStateFlow(iniciais)

    override fun observeAtivas(): Flow<List<RecargaProgramadaVoucherEntity>> =
        recargas.map { list -> list.filter { it.excluidoEm == null } }

    override fun observePorInstrumento(instrumentoPublicId: String): Flow<List<RecargaProgramadaVoucherEntity>> =
        recargas.map { list -> list.filter { it.instrumentoPublicId == instrumentoPublicId && it.excluidoEm == null } }

    override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<RecargaProgramadaVoucherEntity> =
        recargas.value.filter { it.instrumentoPublicId == instrumentoPublicId && it.excluidoEm == null }
            .sortedByDescending { it.dataPrevista }

    override suspend fun listarAtivas(): List<RecargaProgramadaVoucherEntity> = recargas.value.filter { it.excluidoEm == null }

    override suspend fun listarPendentesAtivas(): List<RecargaProgramadaVoucherEntity> =
        recargas.value.filter { it.excluidoEm == null && it.status.name == "PENDENTE" }

    override suspend fun buscarPorCompetencia(instrumentoPublicId: String, competenciaMes: LocalDate): RecargaProgramadaVoucherEntity? =
        recargas.value.firstOrNull {
            it.instrumentoPublicId == instrumentoPublicId && it.competenciaMes == competenciaMes && it.excluidoEm == null
        }

    override suspend fun buscarPorLancamento(lancamentoPublicId: String): List<RecargaProgramadaVoucherEntity> =
        recargas.value.filter { it.lancamentoPublicId == lancamentoPublicId && it.excluidoEm == null }

    override suspend fun buscar(publicId: String): RecargaProgramadaVoucherEntity? = recargas.value.firstOrNull { it.publicId == publicId }

    override suspend fun atualizadasDesde(desde: Instant): List<RecargaProgramadaVoucherEntity> = emptyList()

    override suspend fun upsert(recarga: RecargaProgramadaVoucherEntity) {
        recargas.update { list -> list.filterNot { it.publicId == recarga.publicId } + recarga }
    }

    override suspend fun upsertAll(recargas: List<RecargaProgramadaVoucherEntity>) {
        recargas.forEach { upsert(it) }
    }

    override suspend fun repointarLancamento(de: String, para: String) = Unit

    override suspend fun marcarExcluida(publicId: String, quando: Instant) = Unit

    fun atual(publicId: String): RecargaProgramadaVoucherEntity? = recargas.value.firstOrNull { it.publicId == publicId }
}

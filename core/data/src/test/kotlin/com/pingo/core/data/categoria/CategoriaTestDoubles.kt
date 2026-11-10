package com.pingo.core.data.categoria

import com.pingo.core.auth.StoredTokens
import com.pingo.core.auth.TokenStore
import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.data.impl.RegraCategoriaRepositoryImpl
import com.pingo.core.database.dao.CategoriaDao
import com.pingo.core.database.dao.RegraCategoriaDao
import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.database.entity.RegraCategoriaEntity
import com.pingo.core.model.Categoria
import com.pingo.core.model.TipoCategoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.Base64

internal val AGORA: Instant = Instant.parse("2026-11-10T12:00:00Z")

internal const val USUARIO_PADRAO = "auth0|lucas"

internal class AmbienteCategorias(usuarioSub: String? = USUARIO_PADRAO) {
    val categoriaDao = FakeCategoriaDao()
    val regraDao = FakeRegraCategoriaDao()
    val repositorio = CategoriaRepositoryImpl(categoriaDao)
    val repositorioRegras = RegraCategoriaRepositoryImpl(regraDao)
    private val usuario = UsuarioAutenticado(FakeTokenStore(usuarioSub?.let(::tokensDoUsuario)))
    val seeder = CategoriaSeeder(repositorio, usuario)
    val aprendida = CategoriaAprendida(repositorio, repositorioRegras, usuario)

    suspend fun comPadroes(): AmbienteCategorias {
        seeder.garantirPadroes(AGORA)
        return this
    }

    suspend fun criar(nome: String, tipo: TipoCategoria = TipoCategoria.DESPESA): Categoria =
        (repositorio.criar(nome, tipo, AGORA) as ResultadoCategoria.Sucesso).categoria

    suspend fun idDe(nome: String, tipo: TipoCategoria = TipoCategoria.DESPESA): String =
        repositorio.observeCategorias().first().single { it.nome == nome && it.tipo == tipo }.publicId
}

internal class FakeCategoriaDao(categorias: List<CategoriaEntity> = emptyList()) : CategoriaDao {
    private val tabela = MutableStateFlow(categorias)

    override fun observeAtivas(): Flow<List<CategoriaEntity>> =
        tabela.map { categorias -> categorias.filter { it.excluidoEm == null }.sortedBy { it.nome } }

    override suspend fun buscar(publicId: String): CategoriaEntity? = tabela.value.firstOrNull { it.publicId == publicId }

    override suspend fun atualizadasDesde(desde: Instant): List<CategoriaEntity> = tabela.value.filter { it.atualizadoEm >= desde }

    override suspend fun upsert(categoria: CategoriaEntity) {
        upsertAll(listOf(categoria))
    }

    override suspend fun upsertAll(categorias: List<CategoriaEntity>) {
        tabela.value = tabela.value.filter { atual -> categorias.none { it.publicId == atual.publicId } } + categorias
    }

    override suspend fun marcarExcluida(publicId: String, quando: Instant) {
        tabela.value = tabela.value.map { if (it.publicId == publicId) it.copy(excluidoEm = quando, atualizadoEm = quando) else it }
    }
}

internal class FakeRegraCategoriaDao(regras: List<RegraCategoriaEntity> = emptyList()) : RegraCategoriaDao {
    private val tabela = MutableStateFlow(regras)

    override fun observeAtivas(): Flow<List<RegraCategoriaEntity>> =
        tabela.map { regras -> regras.filter { it.excluidoEm == null }.sortedBy { it.estabelecimentoNormalizado } }

    override suspend fun buscar(publicId: String): RegraCategoriaEntity? = tabela.value.firstOrNull { it.publicId == publicId }

    override suspend fun buscarPorEstabelecimento(estabelecimento: String): RegraCategoriaEntity? = tabela.value
        .filter { it.estabelecimentoNormalizado == estabelecimento && it.excluidoEm == null }
        .minByOrNull { it.publicId }

    override suspend fun atualizadasDesde(desde: Instant): List<RegraCategoriaEntity> = tabela.value.filter { it.atualizadoEm >= desde }

    override suspend fun upsert(regra: RegraCategoriaEntity) {
        upsertAll(listOf(regra))
    }

    override suspend fun upsertAll(regras: List<RegraCategoriaEntity>) {
        tabela.value = tabela.value.filter { atual -> regras.none { it.publicId == atual.publicId } } + regras
    }

    override suspend fun marcarExcluida(publicId: String, quando: Instant) {
        tabela.value = tabela.value.map { if (it.publicId == publicId) it.copy(excluidoEm = quando, atualizadoEm = quando) else it }
    }
}

internal fun categoriaEntidade(
    publicId: String,
    nome: String,
    tipo: TipoCategoria = TipoCategoria.DESPESA,
    arquivada: Boolean = false,
    excluidoEm: Instant? = null,
): CategoriaEntity = CategoriaEntity(
    publicId = publicId,
    nome = nome,
    tipo = tipo,
    padrao = false,
    arquivada = arquivada,
    criadoEm = AGORA,
    atualizadoEm = AGORA,
    excluidoEm = excluidoEm,
)

internal class FakeTokenStore(private var tokens: StoredTokens?) : TokenStore {
    override suspend fun read(): StoredTokens? = tokens

    override suspend fun write(tokens: StoredTokens) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        tokens = null
    }
}

internal fun tokensDoUsuario(sub: String): StoredTokens =
    StoredTokens(accessToken = jwtComPayload("""{"sub":"$sub"}"""), refreshToken = null, expiresAtEpochMillis = Long.MAX_VALUE)

private fun jwtComPayload(payload: String): String {
    val codificador = Base64.getUrlEncoder().withoutPadding()
    val cabecalho = codificador.encodeToString("""{"alg":"RS256"}""".toByteArray())

    return listOf(cabecalho, codificador.encodeToString(payload.toByteArray()), "assinatura").joinToString(".")
}

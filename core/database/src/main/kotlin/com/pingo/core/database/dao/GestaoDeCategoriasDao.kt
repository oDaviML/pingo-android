package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import java.time.Instant

data class LancamentoCategoriaEstabelecimentoRow(val categoriaPublicId: String?, val estabelecimento: String?)

@Dao
abstract class GestaoDeCategoriasDao {
    @Query("SELECT categoriaPublicId, estabelecimento FROM lancamentos WHERE excluidoEm IS NULL")
    abstract fun observeLancamentosParaContagem(): kotlinx.coroutines.flow.Flow<List<LancamentoCategoriaEstabelecimentoRow>>

    @Query("SELECT COUNT(*) FROM lancamentos WHERE categoriaPublicId = :categoriaPublicId AND excluidoEm IS NULL")
    abstract suspend fun contarLancamentosAtivosDaCategoria(categoriaPublicId: String): Int

    @Query("SELECT COUNT(*) FROM regras_categoria WHERE categoriaPublicId = :categoriaPublicId AND excluidoEm IS NULL")
    abstract suspend fun contarRegrasAtivasDaCategoria(categoriaPublicId: String): Int

    @Query(
        """
        UPDATE lancamentos
        SET categoriaPublicId = :destinoPublicId, atualizadoEm = :quando
        WHERE categoriaPublicId = :origemPublicId AND excluidoEm IS NULL
        """,
    )
    abstract suspend fun reatribuirLancamentos(origemPublicId: String, destinoPublicId: String, quando: Instant)

    @Query(
        """
        UPDATE regras_categoria
        SET categoriaPublicId = :destinoPublicId, atualizadoEm = :quando
        WHERE categoriaPublicId = :origemPublicId AND excluidoEm IS NULL
        """,
    )
    abstract suspend fun reatribuirRegras(origemPublicId: String, destinoPublicId: String, quando: Instant)

    @Query(
        """
        UPDATE regras_categoria
        SET excluidoEm = :quando, atualizadoEm = :quando
        WHERE categoriaPublicId = :categoriaPublicId AND excluidoEm IS NULL
        """,
    )
    abstract suspend fun excluirRegrasDaCategoria(categoriaPublicId: String, quando: Instant)

    @Query(
        """
        UPDATE categorias
        SET excluidoEm = :quando, atualizadoEm = :quando
        WHERE publicId = :publicId AND excluidoEm IS NULL
        """,
    )
    abstract suspend fun marcarCategoriaExcluida(publicId: String, quando: Instant)

    @Transaction
    open suspend fun excluirCategoriaAtomicamente(origemPublicId: String, destinoPublicId: String?, quando: Instant) {
        if (destinoPublicId != null) {
            reatribuirLancamentos(origemPublicId, destinoPublicId, quando)
            reatribuirRegras(origemPublicId, destinoPublicId, quando)
        } else {
            excluirRegrasDaCategoria(origemPublicId, quando)
        }
        marcarCategoriaExcluida(origemPublicId, quando)
    }
}

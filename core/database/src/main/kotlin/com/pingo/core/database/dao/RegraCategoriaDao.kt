package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.RegraCategoriaEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface RegraCategoriaDao {
    @Query("SELECT * FROM regras_categoria WHERE excluidoEm IS NULL ORDER BY estabelecimentoNormalizado")
    fun observeAtivas(): Flow<List<RegraCategoriaEntity>>

    @Query("SELECT * FROM regras_categoria WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): RegraCategoriaEntity?

    @Query(
        """
        SELECT * FROM regras_categoria
        WHERE estabelecimentoNormalizado = :estabelecimento AND excluidoEm IS NULL
        ORDER BY publicId LIMIT 1
        """,
    )
    suspend fun buscarPorEstabelecimento(estabelecimento: String): RegraCategoriaEntity?

    @Query("SELECT * FROM regras_categoria WHERE atualizadoEm >= :desde")
    suspend fun atualizadasDesde(desde: Instant): List<RegraCategoriaEntity>

    @Upsert
    suspend fun upsert(regra: RegraCategoriaEntity)

    @Upsert
    suspend fun upsertAll(regras: List<RegraCategoriaEntity>)

    @Query("UPDATE regras_categoria SET excluidoEm = :quando, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarExcluida(publicId: String, quando: Instant)
}

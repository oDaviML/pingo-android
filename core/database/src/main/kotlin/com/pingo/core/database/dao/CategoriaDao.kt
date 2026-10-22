package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.CategoriaEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias WHERE excluidoEm IS NULL ORDER BY nome")
    fun observeAtivas(): Flow<List<CategoriaEntity>>

    @Query("SELECT * FROM categorias WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): CategoriaEntity?

    @Query("SELECT * FROM categorias WHERE atualizadoEm >= :desde")
    suspend fun atualizadasDesde(desde: Instant): List<CategoriaEntity>

    @Upsert
    suspend fun upsert(categoria: CategoriaEntity)

    @Upsert
    suspend fun upsertAll(categorias: List<CategoriaEntity>)

    @Query("UPDATE categorias SET excluidoEm = :quando, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarExcluida(publicId: String, quando: Instant)
}

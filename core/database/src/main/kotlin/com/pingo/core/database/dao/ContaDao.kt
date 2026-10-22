package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.ContaEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ContaDao {
    @Query("SELECT * FROM contas WHERE excluidoEm IS NULL ORDER BY apelido")
    fun observeAtivas(): Flow<List<ContaEntity>>

    @Query("SELECT * FROM contas WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): ContaEntity?

    @Query("SELECT * FROM contas WHERE atualizadoEm >= :desde")
    suspend fun atualizadasDesde(desde: Instant): List<ContaEntity>

    @Upsert
    suspend fun upsert(conta: ContaEntity)

    @Upsert
    suspend fun upsertAll(contas: List<ContaEntity>)

    @Query("UPDATE contas SET excluidoEm = :quando, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarExcluida(publicId: String, quando: Instant)
}

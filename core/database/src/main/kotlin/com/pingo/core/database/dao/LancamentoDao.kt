package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.LancamentoEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface LancamentoDao {
    @Query("SELECT * FROM lancamentos WHERE excluidoEm IS NULL ORDER BY dataHora DESC")
    fun observeAtivos(): Flow<List<LancamentoEntity>>

    @Query("SELECT * FROM lancamentos WHERE excluidoEm IS NULL AND dataHora BETWEEN :inicio AND :fim ORDER BY dataHora DESC")
    fun observePorPeriodo(inicio: Instant, fim: Instant): Flow<List<LancamentoEntity>>

    @Query("SELECT * FROM lancamentos WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): LancamentoEntity?

    @Query("SELECT * FROM lancamentos WHERE hashDuplicata = :hash AND excluidoEm IS NULL LIMIT 1")
    suspend fun buscarPorHash(hash: String): LancamentoEntity?

    @Query("SELECT * FROM lancamentos WHERE atualizadoEm >= :desde")
    suspend fun atualizadosDesde(desde: Instant): List<LancamentoEntity>

    @Upsert
    suspend fun upsert(lancamento: LancamentoEntity)

    @Upsert
    suspend fun upsertAll(lancamentos: List<LancamentoEntity>)

    @Query("UPDATE lancamentos SET excluidoEm = :quando, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarExcluido(publicId: String, quando: Instant)

    @Query("DELETE FROM lancamentos")
    suspend fun limpar()
}

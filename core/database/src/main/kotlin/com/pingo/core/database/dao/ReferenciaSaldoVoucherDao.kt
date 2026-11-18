package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.ReferenciaSaldoVoucherEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ReferenciaSaldoVoucherDao {
    @Query("SELECT * FROM referencias_saldo_voucher WHERE excluidoEm IS NULL ORDER BY instanteReferencia ASC, publicId ASC")
    fun observeAtivas(): Flow<List<ReferenciaSaldoVoucherEntity>>

    @Query(
        """
        SELECT * FROM referencias_saldo_voucher
        WHERE instrumentoPublicId = :instrumentoPublicId AND excluidoEm IS NULL
        ORDER BY instanteReferencia ASC, publicId ASC
        """,
    )
    fun observePorInstrumento(instrumentoPublicId: String): Flow<List<ReferenciaSaldoVoucherEntity>>

    @Query(
        """
        SELECT * FROM referencias_saldo_voucher
        WHERE instrumentoPublicId = :instrumentoPublicId AND excluidoEm IS NULL
        ORDER BY instanteReferencia ASC, publicId ASC
        """,
    )
    suspend fun listarPorInstrumento(instrumentoPublicId: String): List<ReferenciaSaldoVoucherEntity>

    @Query("SELECT * FROM referencias_saldo_voucher WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): ReferenciaSaldoVoucherEntity?

    @Query("SELECT * FROM referencias_saldo_voucher WHERE lancamentoPublicId = :lancamentoPublicId AND excluidoEm IS NULL LIMIT 1")
    suspend fun buscarPorLancamento(lancamentoPublicId: String): ReferenciaSaldoVoucherEntity?

    @Query("SELECT * FROM referencias_saldo_voucher WHERE lancamentoPublicId = :lancamentoPublicId LIMIT 1")
    suspend fun buscarPorLancamentoIncluindoExcluidas(lancamentoPublicId: String): ReferenciaSaldoVoucherEntity?

    @Query("SELECT * FROM referencias_saldo_voucher WHERE atualizadoEm >= :desde")
    suspend fun atualizadasDesde(desde: Instant): List<ReferenciaSaldoVoucherEntity>

    @Upsert
    suspend fun upsert(referencia: ReferenciaSaldoVoucherEntity)

    @Upsert
    suspend fun upsertAll(referencias: List<ReferenciaSaldoVoucherEntity>)

    @Query("UPDATE referencias_saldo_voucher SET lancamentoPublicId = :para WHERE lancamentoPublicId = :de")
    suspend fun repointarLancamento(de: String, para: String)

    @Query("UPDATE referencias_saldo_voucher SET excluidoEm = :quando, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarExcluida(publicId: String, quando: Instant)
}

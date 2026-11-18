package com.pingo.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface RecargaProgramadaVoucherDao {
    @Query("SELECT * FROM recargas_programadas_voucher WHERE excluidoEm IS NULL ORDER BY dataPrevista DESC, publicId ASC")
    fun observeAtivas(): Flow<List<RecargaProgramadaVoucherEntity>>

    @Query(
        """
        SELECT * FROM recargas_programadas_voucher
        WHERE instrumentoPublicId = :instrumentoPublicId AND excluidoEm IS NULL
        ORDER BY dataPrevista DESC, publicId ASC
        """,
    )
    fun observePorInstrumento(instrumentoPublicId: String): Flow<List<RecargaProgramadaVoucherEntity>>

    @Query(
        """
        SELECT * FROM recargas_programadas_voucher
        WHERE instrumentoPublicId = :instrumentoPublicId AND excluidoEm IS NULL
        ORDER BY dataPrevista DESC, publicId ASC
        """,
    )
    suspend fun listarPorInstrumento(instrumentoPublicId: String): List<RecargaProgramadaVoucherEntity>

    @Query("SELECT * FROM recargas_programadas_voucher WHERE excluidoEm IS NULL ORDER BY dataPrevista DESC, publicId ASC")
    suspend fun listarAtivas(): List<RecargaProgramadaVoucherEntity>

    @Query("SELECT * FROM recargas_programadas_voucher WHERE excluidoEm IS NULL AND status = 'PENDENTE' ORDER BY dataPrevista ASC")
    suspend fun listarPendentesAtivas(): List<RecargaProgramadaVoucherEntity>

    @Query(
        """
        SELECT * FROM recargas_programadas_voucher
        WHERE instrumentoPublicId = :instrumentoPublicId AND competenciaMes = :competenciaMes AND excluidoEm IS NULL
        LIMIT 1
        """,
    )
    suspend fun buscarPorCompetencia(instrumentoPublicId: String, competenciaMes: LocalDate): RecargaProgramadaVoucherEntity?

    @Query("SELECT * FROM recargas_programadas_voucher WHERE lancamentoPublicId = :lancamentoPublicId AND excluidoEm IS NULL")
    suspend fun buscarPorLancamento(lancamentoPublicId: String): List<RecargaProgramadaVoucherEntity>

    @Query("SELECT * FROM recargas_programadas_voucher WHERE publicId = :publicId")
    suspend fun buscar(publicId: String): RecargaProgramadaVoucherEntity?

    @Query("SELECT * FROM recargas_programadas_voucher WHERE atualizadoEm >= :desde")
    suspend fun atualizadasDesde(desde: Instant): List<RecargaProgramadaVoucherEntity>

    @Upsert
    suspend fun upsert(recarga: RecargaProgramadaVoucherEntity)

    @Upsert
    suspend fun upsertAll(recargas: List<RecargaProgramadaVoucherEntity>)

    @Query("UPDATE recargas_programadas_voucher SET lancamentoPublicId = :para WHERE lancamentoPublicId = :de")
    suspend fun repointarLancamento(de: String, para: String)

    @Query("UPDATE recargas_programadas_voucher SET excluidoEm = :quando, atualizadoEm = :quando WHERE publicId = :publicId")
    suspend fun marcarExcluida(publicId: String, quando: Instant)
}

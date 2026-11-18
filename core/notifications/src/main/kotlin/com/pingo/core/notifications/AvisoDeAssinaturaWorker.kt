package com.pingo.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate

@HiltWorker
class AvisoDeAssinaturaWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val executor: ExecutorDeAvisoDeAssinatura,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        executor.executar(
            assinaturaPublicId = inputData.getString(CHAVE_ASSINATURA_PUBLIC_ID),
            dataPrevistaIso = inputData.getString(CHAVE_DATA_PREVISTA_ISO),
            alvoEpochMillis = inputData.getLong(CHAVE_ALVO_EPOCH_MILLIS, Long.MIN_VALUE),
        )
        return Result.success()
    }

    companion object {
        const val CHAVE_ASSINATURA_PUBLIC_ID = "assinaturaPublicId"
        const val CHAVE_DATA_PREVISTA_ISO = "dataPrevistaIso"
        const val CHAVE_ALVO_EPOCH_MILLIS = "alvoEpochMillis"

        fun dadosDeEntrada(assinaturaPublicId: String, proximaCobrancaEstimada: LocalDate, alvo: Instant): Data = Data.Builder()
            .putString(CHAVE_ASSINATURA_PUBLIC_ID, assinaturaPublicId)
            .putString(CHAVE_DATA_PREVISTA_ISO, proximaCobrancaEstimada.toString())
            .putLong(CHAVE_ALVO_EPOCH_MILLIS, alvo.toEpochMilli())
            .build()
    }
}

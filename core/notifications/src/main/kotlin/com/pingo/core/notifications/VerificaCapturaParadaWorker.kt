package com.pingo.core.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class VerificaCapturaParadaWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val gatilhoDeCapturaParada: GatilhoDeCapturaParada,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        runCatching { gatilhoDeCapturaParada.verificar() }
        return Result.success()
    }
}

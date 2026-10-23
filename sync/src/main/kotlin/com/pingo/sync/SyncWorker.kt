package com.pingo.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val runner: SyncRunner,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        runner.run()
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() },
    )
}

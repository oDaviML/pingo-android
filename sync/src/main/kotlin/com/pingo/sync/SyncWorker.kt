package com.pingo.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pingo.core.network.PingoApiException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val runner: SyncRunner,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        runner.run()
        Result.success()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: PingoApiException) {
        if (exception.statusCode in CLIENT_ERROR_RANGE) Result.failure() else Result.retry()
    } catch (exception: Exception) {
        Result.retry()
    }

    private companion object {
        val CLIENT_ERROR_RANGE = 400..499
    }
}

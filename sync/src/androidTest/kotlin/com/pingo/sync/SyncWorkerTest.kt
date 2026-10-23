package com.pingo.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `sincronizacao bem sucedida conclui o worker`() {
        val result = runBlocking {
            buildWorker(SyncRunner { }).doWork()
        }

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `falha de sincronizacao pede retry com backoff`() {
        val result = runBlocking {
            buildWorker(SyncRunner { throw IOException("sem rede") }).doWork()
        }

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    private fun buildWorker(runner: SyncRunner): SyncWorker = TestListenableWorkerBuilder<SyncWorker>(context)
        .setWorkerFactory(
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker? = if (workerClassName == SyncWorker::class.java.name) {
                    SyncWorker(appContext, workerParameters, runner)
                } else {
                    null
                }
            },
        )
        .build()
}

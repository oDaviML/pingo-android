package com.pingo.spike.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SpikeListenerService : NotificationListenerService() {
    private val store by lazy { CorpusStore(File(filesDir, CorpusStore.DIRECTORY)) }
    private val preferences by lazy { SpikePreferences(this) }
    private val writer: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        if (!preferences.captureAllPackages && !KnownApps.isKnown(statusBarNotification.packageName)) {
            return
        }

        val extras = statusBarNotification.notification.extras
        val entry =
            CorpusEntry(
                packageName = statusBarNotification.packageName,
                postedAt = statusBarNotification.postTime,
                capturedAt = System.currentTimeMillis(),
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
                bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty(),
            )

        if (!writer.isShutdown) {
            writer.execute { store.append(entry) }
        }
    }

    override fun onDestroy() {
        writer.shutdown()
        super.onDestroy()
    }
}

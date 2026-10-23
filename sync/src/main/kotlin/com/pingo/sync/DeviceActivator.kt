package com.pingo.sync

import android.os.Build
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.network.PingoApiClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceActivator @Inject constructor(private val api: PingoApiClient, private val preferences: UserPreferencesDataSource) {
    private val mutex = Mutex()
    private var activated = false

    suspend fun activate() {
        if (activated) {
            return
        }

        mutex.withLock {
            if (activated) {
                return
            }

            api.activateDevice(
                deviceId = preferences.deviceId(),
                deviceName = Build.MODEL,
            )

            activated = true
        }
    }
}

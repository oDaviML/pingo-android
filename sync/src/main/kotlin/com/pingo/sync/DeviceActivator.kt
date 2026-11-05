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

    suspend fun activate() {
        mutex.withLock {
            if (preferences.consentVersion() == null) {
                return
            }

            val deviceId = preferences.deviceId()

            if (preferences.activatedDeviceId() == deviceId) {
                return
            }

            api.activateDevice(
                deviceId = deviceId,
                deviceName = Build.MODEL,
            )

            preferences.setActivatedDeviceId(deviceId)
        }
    }
}

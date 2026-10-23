package com.pingo.core.datastore

import kotlinx.coroutines.flow.Flow
import java.time.Instant

enum class SyncStatus {
    IDLE,
    RUNNING,
    FAILED,
}

interface UserPreferencesDataSource {
    val syncStatus: Flow<SyncStatus>

    suspend fun setSyncStatus(status: SyncStatus)

    suspend fun pullCursor(): String?

    suspend fun setPullCursor(cursor: String?)

    suspend fun pushCursor(): Instant?

    suspend fun setPushCursor(instant: Instant)

    suspend fun lastSyncAt(): Instant?

    suspend fun setLastSyncAt(instant: Instant)
}

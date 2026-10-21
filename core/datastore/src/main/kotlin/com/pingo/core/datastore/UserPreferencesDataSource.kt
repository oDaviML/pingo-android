package com.pingo.core.datastore

import kotlinx.coroutines.flow.Flow

enum class SyncStatus {
    IDLE,
    RUNNING,
    FAILED,
}

interface UserPreferencesDataSource {
    val syncStatus: Flow<SyncStatus>
}

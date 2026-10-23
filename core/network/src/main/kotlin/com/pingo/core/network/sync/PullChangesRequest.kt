package com.pingo.core.network.sync

import kotlinx.serialization.Serializable

@Serializable
data class PullChangesRequest(val cursor: String? = null)

package com.pingo.core.network

import kotlinx.serialization.Serializable

@Serializable
data class ActivateDeviceRequestDto(val deviceId: String, val deviceName: String? = null)

@Serializable
data class ActivateDeviceResponseDto(val deviceId: String? = null, val deviceName: String? = null, val activatedAt: String? = null)

data class ApiDevice(val deviceId: String?, val deviceName: String?, val activatedAt: String?)

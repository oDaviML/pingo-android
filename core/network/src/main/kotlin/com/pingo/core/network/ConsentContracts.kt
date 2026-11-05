package com.pingo.core.network

import kotlinx.serialization.Serializable

@Serializable
data class RecordConsentRequestDto(val versao: String)

@Serializable
data class RecordConsentResponseDto(val consentidoEm: String? = null, val versao: String? = null)

package com.pingo.core.network

class PingoApiException(val statusCode: Int, message: String) : Exception(message)

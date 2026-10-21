package com.pingo.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: Throwable) : AppResult<Nothing>
}

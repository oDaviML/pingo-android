package com.pingo.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class AppResultTest {
    @Test
    fun successCarriesData() {
        val result: AppResult<String> = AppResult.Success("ok")
        assertEquals("ok", (result as AppResult.Success).data)
    }

    @Test
    fun failureCarriesError() {
        val error = IllegalStateException("falha")
        val result: AppResult<String> = AppResult.Failure(error)
        assertEquals(error, (result as AppResult.Failure).error)
    }
}

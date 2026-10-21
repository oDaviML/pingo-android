package com.pingo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackageNameTest {
    @Test
    fun targetContextUsesPingoPackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.pingo", context.packageName)
    }
}

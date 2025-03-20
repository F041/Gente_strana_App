package com.gentestrana

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config() // Use Config.DEFAULT
class MinimalRobolectricTest {

    @Test
    fun minimalRobolectricTest_shouldRun() {
        // This test just checks if Robolectric setup is working without errors
        println("Minimal Robolectric Test is running!") // Add a print statement to see if it runs
        assert(true) // Simple assertion that should always pass
    }
}
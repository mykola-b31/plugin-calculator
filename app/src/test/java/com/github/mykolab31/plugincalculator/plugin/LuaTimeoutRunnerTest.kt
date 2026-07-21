package com.github.mykolab31.plugincalculator.plugin

import org.junit.Assert.*
import org.junit.Test

class LuaTimeoutRunnerTest {

    @Test
    fun `fast block completes with no timeout message`() {
        var ran = false
        val message = LuaTimeoutRunner.runOrTimeoutMessage(2) {
            ran = true
        }

        assertNull(message)
        assertTrue(ran)
    }

    @Test
    fun `block that hangs past the timeout is reported and interrupted`() {
        val start = System.currentTimeMillis()
        val message = LuaTimeoutRunner.runOrTimeoutMessage(1) {
            Thread.sleep(10_000)
        }
        val elapsed = System.currentTimeMillis() - start

        assertNotNull(message)
        assertTrue(message!!.contains("exceeded", ignoreCase = true))
        assertTrue(
            "Expected to return shortly after the 1s timeout, took ${elapsed}ms",
            elapsed < 3000
        )
    }

    @Test
    fun `exception thrown inside block is reported as an execution error`() {
        val message = LuaTimeoutRunner.runOrTimeoutMessage(2) {
            throw IllegalArgumentException("boom")
        }

        assertNotNull(message)
        assertTrue(message!!.contains("boom"))
    }
}
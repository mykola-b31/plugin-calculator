package com.github.mykolab31.plugincalculator.plugin

import com.github.mykolab31.plugincalculator.data.model.CalculationResult
import org.junit.Test
import org.junit.Assert.*

class PluginExecutorTest {

    private val executor = PluginExecutor()

    private val arithmeticPlugin = """
        function execute(operation, args)
            if operation == "add" then
                return args[1] + args[2]
            elseif operation == "subtract" then
                return args[1] - args[2]
            elseif operation == "multiply" then
                return args[1] * args[2]
            elseif operation == "divide" then
                if args[2] == 0 then
                    return { type = "error", message = "Division by zero" }
                end
                return args[1] / args[2]
            end
        end
    """.trimIndent()

    @Test
    fun `addition returns correct result`() {
        val result = executor.execute(arithmeticPlugin, "add", listOf(2.0, 3.0))
        assertTrue(result is PluginExecutionResult.Success)
        val value = (result as PluginExecutionResult.Success).result as CalculationResult.Number
        assertEquals(5.0, value.value, 0.0001)
    }

    @Test
    fun `division by zero returns error`() {
        val result = executor.execute(arithmeticPlugin, "divide", listOf(10.0, 0.0))
        assertTrue(result is PluginExecutionResult.Error)
        assertEquals("Division by zero", (result as PluginExecutionResult.Error).message)
    }

    @Test
    fun `missing execute function returns error`() {
        val brokenPlugin = "function notExecute() end"
        val result = executor.execute(brokenPlugin, "add", listOf(1.0, 1.0))
        assertTrue(result is PluginExecutionResult.Error)
    }

    @Test
    fun `infinite loop times out`() {
        val infinitePlugin = """
            function execute(operation, args)
                while true do end
            end
        """.trimIndent()
        val result = executor.execute(infinitePlugin, "loop", listOf())
        assertTrue(result is PluginExecutionResult.Error)
        assertTrue((result as PluginExecutionResult.Error).message.contains("timed out"))
    }

    @Test
    fun `sandbox blocks io access`() {
        val maliciousPlugin = """
            function execute(operation, args)
                if io then
                    return 999
                end
                return -1
            end
        """.trimIndent()
        val result = executor.execute(maliciousPlugin, "test", listOf())
        val value = (result as PluginExecutionResult.Success).result as CalculationResult.Number
        assertEquals(-1.0, value.value, 0.0001)
    }
}
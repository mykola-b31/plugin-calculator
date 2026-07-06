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

    @Test
    fun `infinite loop is stopped by time limit`() {
        val executor = PluginExecutor()
        val infiniteLoopScript = """
            function execute(op, args)
                local x = 0
                while true do
                    x = x + 1
                 end
                 return x
            end
        """.trimIndent()

        val start = System.currentTimeMillis()
        val result = executor.execute(infiniteLoopScript, "loop", listOf(1.0))
        val elapsed = System.currentTimeMillis() - start

        assertTrue(result is PluginExecutionResult.Error)
        assertTrue(
            "Expected time-limit error, got: ${(result as PluginExecutionResult.Error).message}",
            result.message.contains("execution time", ignoreCase = true)
        )
        assertTrue(
            "Expected time limit (~2000ms) to trigger, took ${elapsed}ms",
            elapsed < 3000
        )
        println("Time limit triggered after ${elapsed}ms")
    }
}
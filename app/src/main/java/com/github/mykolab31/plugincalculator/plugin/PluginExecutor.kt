package com.github.mykolab31.plugincalculator.plugin

import com.github.mykolab31.plugincalculator.data.model.CalculationResult
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import java.math.BigDecimal
import kotlin.math.abs

class PluginExecutor {

    companion object {
        private const val EXECUTION_TIMEOUT_SECONDS = 3L
        private const val MAX_SAFE_INTEGER = 9_007_199_254_740_991.0
    }

    /**
     * Execute the execute(operation, args) function in the plugin script
     *
     * @param script text of the plugin Lua file
     * @param operation id of the operation from the manifest
     * @param args list of numeric arguments
     * @return execution result as LuaValue
     */
    fun execute(script: String, operation: String, args: List<BigDecimal>): PluginExecutionResult {
        var result: PluginExecutionResult = PluginExecutionResult.Error("Plugin did not produce a result")

        val timeoutMessage = LuaTimeoutRunner.runOrTimeoutMessage(EXECUTION_TIMEOUT_SECONDS) {
            result = runScript(script, operation, args)
        }

        return timeoutMessage?.let { PluginExecutionResult.Error("Plugin execution timed out: $it") } ?: result
    }


    fun runScript(script: String, operation: String, args: List<BigDecimal>): PluginExecutionResult {
        return try {
            val globals = LuaSandbox.create()
            globals.load(script, "plugin").call()

            val executeFunc = globals.get("execute")
            if (executeFunc.isnil()) {
                return PluginExecutionResult.Error("Plugin does not define 'execute' function")
            }

            val luaArgs = LuaTable()
            args.forEachIndexed { index, value ->
                luaArgs.set(index + 1, LuaValue.valueOf(value.toDouble()))
            }

            val result = executeFunc.call(LuaValue.valueOf(operation), luaArgs)
            convertResult(result)

        } catch (e: LuaError) {
            PluginExecutionResult.Error("Lua error: ${e.message}")
        } catch (e: Exception) {
            PluginExecutionResult.Error("Exception error: ${e.message}")
        }
    }

    /**
     * Converts the LuaValue returned by the plugin
     * to a CalculationResult for display in the UI
     */
    private fun convertResult(result: LuaValue): PluginExecutionResult {
        return when {
            result.isnumber() -> convertNumberResult(result.todouble())
            result.istable() -> convertTableResult(result.checktable())
            else -> PluginExecutionResult.Error("Unsupported return type from plugin")
        }
    }

    private fun convertNumberResult(value: Double): PluginExecutionResult {
        if (!value.isFinite()) {
            return PluginExecutionResult.Error("Plugin returned a non-finite number")
        }

        val decimalValue = BigDecimal.valueOf(value)

        val isApproximate = abs(value) > MAX_SAFE_INTEGER || BigDecimal(value).compareTo(decimalValue) != 0
        return PluginExecutionResult.Success(
            CalculationResult.Number(
                value = decimalValue,
                isApproximate = isApproximate
            )
        )
    }

    private fun convertTableResult(table: LuaTable): PluginExecutionResult {
        val type = table.get("type")
        return when(type.tojstring()) {
            "matrix" -> {
                val data = table.get("data").checktable()
                val rows = mutableListOf<List<BigDecimal>>()
                for (i in 1..data.length()) {
                    val row = data.get(i).checktable()
                    val rowValues = (1..row.length()).map { BigDecimal.valueOf(row.get(it).todouble()) }
                    rows.add(rowValues)
                }
                PluginExecutionResult.Success(CalculationResult.Matrix(rows))
            }
            "error" -> PluginExecutionResult.Error(table.get("message").tojstring())
            else -> PluginExecutionResult.Error("Unknown result type: ${type.tojstring()}")
        }
    }

}

sealed class PluginExecutionResult {
    data class Success(val result: CalculationResult) : PluginExecutionResult()
    data class Error(val message: String) : PluginExecutionResult()
}
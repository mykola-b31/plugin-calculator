package com.github.mykolab31.plugincalculator.plugin

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import com.github.mykolab31.plugincalculator.data.model.CalculationResult
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib
import org.luaj.vm2.lib.jse.JseMathLib

class PluginExecutor {

    private val executorService = Executors.newSingleThreadExecutor()

    companion object {
        private const val EXECUTION_TIMEOUT_SECONDS = 3L
    }

    /**
     * Create an isolated Lua-environment without access
     * to the file system, OS, and Java reflection
     */
    private fun createSandbox(): Globals {
        val globals = Globals()
        globals.load(BaseLib())
        globals.load(PackageLib())
        globals.load(TableLib())
        globals.load(StringLib())
        globals.load(JseMathLib())

        LuaC.install(globals)

        globals.set("load", LuaValue.NIL)
        globals.set("loadfile", LuaValue.NIL)
        globals.set("dofile", LuaValue.NIL)
        globals.set("require", LuaValue.NIL)
        globals.set("collectgarbage", LuaValue.NIL)

        return globals
    }

    /**
     * Execute the execute(operation, args) function in the plugin script
     *
     * @param script text of the plugin Lua file
     * @param operation id of the operation from the manifest
     * @param args list of numeric arguments
     * @return execution result as LuaValue
     */
    fun execute(script: String, operation: String, args: List<Double>): PluginExecutionResult {
        val future = executorService.submit<PluginExecutionResult> {
            runScript(script, operation, args)
        }

        return try {
            future.get(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            PluginExecutionResult.Error("Plugin execution timed out")
        } catch (e: Exception) {
            PluginExecutionResult.Error("Execution error: ${e.message}")
        }
    }


    fun runScript(script: String, operation: String, args: List<Double>): PluginExecutionResult {
        return try {
            val globals = createSandbox()
            globals.load(script, "plugin").call()

            val executeFunc = globals.get("execute")
            if (executeFunc.isnil()) {
                return PluginExecutionResult.Error("Plugin does not define 'execute' function")
            }

            val luaArgs = LuaTable()
            args.forEachIndexed { index, value ->
                luaArgs.set(index + 1, LuaValue.valueOf(value))
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
     * Converts the LuaValur returned by the plugin
     * to a CalculationResult for display in the UI
     */
    private fun convertResult(result: LuaValue): PluginExecutionResult {
        return when {
            result.isnumber() -> PluginExecutionResult.Success(
                CalculationResult.Number(result.todouble())
            )
            result.istable() -> convertTableResult(result.checktable())
            else -> PluginExecutionResult.Error("Unsupported return type from plugin")
        }
    }

    private fun convertTableResult(table: LuaTable): PluginExecutionResult {
        val type = table.get("type")
        return when(type.tojstring()) {
            "matrix" -> {
                val data = table.get("data").checktable()
                val rows = mutableListOf<List<Double>>()
                for (i in 1..data.length()) {
                    val row = data.get(i).checktable()
                    val rowValues = (1..row.length()).map { row.get(it).todouble() }
                    rows.add(rowValues)
                }
                PluginExecutionResult.Success(CalculationResult.Matrix(rows))
            }
            "error" -> PluginExecutionResult.Error(table.get("message").tojstring())
            else -> PluginExecutionResult.Error("Unknown result type: ${type.tojstring()}")
        }
    }

}

sealed class PluginExecutionResult{
    data class Success(val result: CalculationResult) : PluginExecutionResult()
    data class Error(val message: String) : PluginExecutionResult()
}
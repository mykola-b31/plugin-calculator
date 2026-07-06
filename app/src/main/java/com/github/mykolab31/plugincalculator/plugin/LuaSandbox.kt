package com.github.mykolab31.plugincalculator.plugin

import org.luaj.vm2.Globals
import org.luaj.vm2.Lua
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.DebugLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib
import org.luaj.vm2.lib.jse.JseMathLib
import java.util.concurrent.TimeUnit

object LuaSandbox {

    private val MAX_EXECUTION_TIME_NANOS = TimeUnit.MILLISECONDS.toNanos(2000)
    private const val CHECK_EVERY_N_INSTRUCTIONS = 1000

    /**
     * Creates an isolated Lua environment without access
     * to the file system, OS, or Java reflection.
     */
    fun create(): Globals {
        val globals = Globals()
        globals.load(BaseLib())
        globals.load(PackageLib())
        globals.load(TableLib())
        globals.load(StringLib())
        globals.load(JseMathLib())
        globals.load(TimeLimitDebugLib(MAX_EXECUTION_TIME_NANOS, CHECK_EVERY_N_INSTRUCTIONS))

        LuaC.install(globals)

        globals.set("load", LuaValue.NIL)
        globals.set("loadfile", LuaValue.NIL)
        globals.set("dofile", LuaValue.NIL)
        globals.set("require", LuaValue.NIL)
        globals.set("collectgarbage", LuaValue.NIL)
        globals.set("debug", LuaValue.NIL)

        return globals
    }
}

private class TimeLimitDebugLib(
    private val maxDurationNanos: Long,
    private val checkEveryNInstructions: Int
) : DebugLib() {

    private var startTime = System.nanoTime()
    private var sinceLastCheck = 0

    override fun onInstruction(pc: Int, v: Varargs?, top: Int) {
        sinceLastCheck++
        if (sinceLastCheck >= checkEveryNInstructions) {
            sinceLastCheck = 0
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            if (System.nanoTime() - startTime > maxDurationNanos) {
                throw LuaError(
                    "Plugin exceed maximum execution time — likely an infinite loop or excessive computation"
                )
            }
        }
        super.onInstruction(pc, v, top)
    }
}
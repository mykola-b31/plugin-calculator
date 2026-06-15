package com.github.mykolab31.plugincalculator.plugin

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib
import org.luaj.vm2.lib.jse.JseMathLib

object LuaSandbox {

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

        LuaC.install(globals)

        globals.set("load", LuaValue.NIL)
        globals.set("loadfile", LuaValue.NIL)
        globals.set("dofile", LuaValue.NIL)
        globals.set("require", LuaValue.NIL)
        globals.set("collectgarbage", LuaValue.NIL)

        return globals
    }
}
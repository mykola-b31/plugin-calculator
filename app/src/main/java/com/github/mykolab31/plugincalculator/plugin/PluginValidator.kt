package com.github.mykolab31.plugincalculator.plugin

import com.github.mykolab31.plugincalculator.data.model.Plugin
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.lib.BaseLib

sealed class ValidationResult {
    data class Valid(val plugin: Plugin) : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

class PluginValidator (
    private val manifestParser: ManifestParser = ManifestParser()
) {
    /**
     * Validates a plugin package consisting of a manifest and Lua script
     *
     * @param manifestJson raw content of manifest.json
     * @param script raw content of the entry Lua file
     */
    fun validate(manifestJson: String, script: String): ValidationResult {
        // level 1 - Manifest
        val parseResult = manifestParser.parse(manifestJson)
        val plugin = when (parseResult) {
            is ManifestParseResult.Error -> return ValidationResult.Invalid(
                "Manifest error: ${parseResult.message}"
            )
            is ManifestParseResult.Success -> parseResult.plugin
        }

        // level 2 - Syntax
        val globals = LuaSandbox.create()
        try {
            globals.load(script, "plugin")
        } catch (e: LuaError) {
            return ValidationResult.Invalid("Syntax error in plugin script: ${e.message}")
        }

        // level 3 - Contract
        try {
            globals.load(script, "plugin").call()
        } catch (e: LuaError) {
            return ValidationResult.Invalid("Runtime error while loading plugin: ${e.message}")
        }

        val executeFunc = globals.get("execute")
        if (executeFunc.isnil()) {
            return ValidationResult.Invalid("Plugin script does not define 'execute' function")
        }
        if (!executeFunc.isfunction()) {
            return ValidationResult.Invalid("'execute' is defined but is not a function")
        }

        return ValidationResult.Valid(plugin)
    }

}
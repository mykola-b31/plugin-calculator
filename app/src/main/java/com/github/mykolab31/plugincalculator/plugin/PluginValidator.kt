package com.github.mykolab31.plugincalculator.plugin

import com.github.mykolab31.plugincalculator.data.model.Plugin
import org.luaj.vm2.LuaError

sealed class ValidationResult {
    data class Valid(val plugin: Plugin) : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

class PluginValidator (
    private val manifestParser: ManifestParser = ManifestParser()
) {
    companion object {
        private const val VALIDATION_TIMEOUT_SECONDS = 3L
    }

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

        // level 2 & 3 - Syntax & Contract
        val globals = LuaSandbox.create()
        var validationError: String? = null
        val timeoutMessage = LuaTimeoutRunner.runOrTimeoutMessage(VALIDATION_TIMEOUT_SECONDS) {
            try {
                val chunk = globals.load(script, "plugin")
                try {
                    chunk.call()
                } catch (e: LuaError) {
                    validationError = "Runtime error while loading plugin: ${e.message}"
                }
            } catch (e: LuaError) {
                validationError = "Syntax error in plugin script: ${e.message}"
            }
        }

        if (timeoutMessage != null) {
            return ValidationResult.Invalid("Plugin took too long to load: $timeoutMessage")
        }
        validationError?.let { return ValidationResult.Invalid(it) }

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
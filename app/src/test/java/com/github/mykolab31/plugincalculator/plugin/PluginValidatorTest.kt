package com.github.mykolab31.plugincalculator.plugin

import org.junit.Test
import org.junit.Assert.*

class PluginValidatorTest {

    private val validator = PluginValidator()

    private val validManifest = """
        {
            "id": "com.example.arithmetic",
            "name": "Arithmetic",
            "version": "1.0.0",
            "description": "Basic operations",
            "category": "ARITHMETIC",
            "entryFile": "plugin.lua",
            "operations": [
                { "id": "add", "label": "+", "inputs": 2 }
            ]
        }
    """.trimIndent()

    @Test
    fun `valid plugin passes all checks`() {
        val script = """
            function execute(operation, args)
                return args[1] + args[2]
            end
        """.trimIndent()

        val result = validator.validate(validManifest, script)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `invalid manifest fails at level 1`() {
        val brokenManifest = "{ not valid json }"
        val script = "function execute(operation, args) return 1 end"

        val result = validator.validate(brokenManifest, script)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).reason.startsWith("Manifest error"))
    }

    @Test
    fun `syntax error in script fails at level 2`() {
        val script = "function execute(operation, args return 1 end"

        val result = validator.validate(validManifest, script)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).reason.contains("Syntax error"))
    }

    @Test
    fun `missing execute function fails at level 3`() {
        val script = """
            function notExecute(operation, args)
                return 1
            end
        """.trimIndent()

        val result = validator.validate(validManifest, script)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).reason.contains("does not define 'execute'"))
    }

    @Test
    fun `execute as non-function fails at level 3`() {
        val script = "execute = 33"

        val result = validator.validate(validManifest, script)
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).reason.contains("is not a function"))
    }
}
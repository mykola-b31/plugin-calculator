package com.github.mykolab31.plugincalculator.plugin

import com.github.mykolab31.plugincalculator.data.model.PluginCategory
import org.junit.Test
import org.junit.Assert.*

class ManifestParserTest {

    private val parser = ManifestParser()

    @Test
    fun `valid manifest parses correctly`() {
        val json = """
            {
                "id": "com.example.trigonometry",
                "name": "Trigonometry",
                "version": "1.0.0",
                "description": "Basic trig functions",
                "category": "trigonometry",
                "entryFile": "plugin.lua",
                "operations": [
                    { "id": "sin", "label": "sin(x)", "inputs": 1 }
                ]
            }
        """.trimIndent()

        val result = parser.parse(json)
        assertTrue(result is ManifestParseResult.Success)
        val plugin = (result as ManifestParseResult.Success).plugin
        assertEquals("Trigonometry", plugin.name)
        assertEquals(PluginCategory.TRIGONOMETRY, plugin.category)
        assertEquals(1, plugin.operations.size)
    }

    @Test
    fun `unknown category falls back to OTHER`() {
        val json = """
            {
                "id": "com.example.custom",
                "name": "Custom",
                "version": "1.0.0",
                "description": "Something custom",
                "category": "QUANTUM_PHYSICS",
                "entryFile": "plugin.lua",
                "operations": [
                    { "id": "x", "label": "x", "inputs": 1 }
                ]
            }
        """.trimIndent()

        val result = parser.parse(json)
        assertTrue(result is ManifestParseResult.Success)
        val plugin = (result as ManifestParseResult.Success).plugin
        assertEquals(PluginCategory.OTHER, plugin.category)
    }

    @Test
    fun `empty operations list returns error`() {
        val json = """
            {
                "id": "com.example.empty",
                "name": "Empty",
                "version": "1.0.0",
                "description": "No operations",
                "category": "OTHER",
                "entryFile": "plugin.lua",
                "operations": []
            }
        """.trimIndent()

        val result = parser.parse(json)
        assertTrue(result is ManifestParseResult.Error)
    }

    @Test
    fun `malformed json returns error`() {
        val json = "{ this is not valid json }"
        val result = parser.parse(json)
        assertTrue(result is ManifestParseResult.Error)
    }

    @Test
    fun `missing required fields returns error`() {
        val json = """
            {
                "id": "com.example.incomplete",
                "name": "Incomplete"
            }
        """.trimIndent()

        val result = parser.parse(json)
        assertTrue(result is ManifestParseResult.Error)
    }
}
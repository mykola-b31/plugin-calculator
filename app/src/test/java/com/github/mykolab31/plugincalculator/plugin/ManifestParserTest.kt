package com.github.mykolab31.plugincalculator.plugin

import com.github.mykolab31.plugincalculator.data.model.OperationArity
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
                "author": "John Doe",
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
                "author": "John Doe",
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
                "author": "John Doe",
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

    @Test
    fun `manifest with invalid inputs count is rejected`() {
        val manifestJson = """
            {
                "id": "test-plugin",
                "name": "Test",
                "version": "1.0.0",
                "description": "Test plugin",
                "category": "MATH",
                "entryFile": "main.lua",
                "operations": [
                    { "id": "weird", "label": "weird(x,y,z)", "inputs": 3 }
                ]
            }
        """.trimIndent()

        val result = ManifestParser().parse(manifestJson)

        assertTrue(result is ManifestParseResult.Error)
    }

    @Test
    fun `manifest with zero inputs is accepted as nullary operation`() {
        val manifestJson = """
            {
                "id": "test-plugin",
                "name": "Test",
                "version": "1.0.0",
                "description": "Test plugin",
                "category": "MATH",
                "entryFile": "main.lua",
                "operations": [
                    { "id": "pi", "label": "π", "inputs": 0 }
                ]
            }
        """.trimIndent()

        val result = ManifestParser().parse(manifestJson)

        assertTrue(result is ManifestParseResult.Success)
        val operation = (result as ManifestParseResult.Success).plugin.operations.first()
        assertEquals(OperationArity.NULLARY, operation.arity)
    }

    @Test
    fun `manifest requiring newer app version is rejected`() {
        val parser = ManifestParser(currentAppVersion = "1.0.0")
        val manifestJson = """
        {
            "id": "test-plugin",
            "name": "Test",
            "version": "1.0.0",
            "description": "Test plugin",
            "category": "MATH",
            "entryFile": "main.lua",
            "minAppVersion": "2.0.0",
            "operations": [
                { "id": "sin", "label": "sin(x)", "inputs": 1 }
            ]
        }
    """.trimIndent()

        val result = parser.parse(manifestJson)

        assertTrue(result is ManifestParseResult.Error)
        assertTrue((result as ManifestParseResult.Error).message.contains("2.0.0"))
    }

    @Test
    fun `manifest with compatible minAppVersion is accepted`() {
        val parser = ManifestParser(currentAppVersion = "2.5.0")
        val manifestJson = """
        {
            "id": "test-plugin",
            "name": "Test",
            "version": "1.0.0",
            "description": "Test plugin",
            "category": "MATH",
            "entryFile": "main.lua",
            "minAppVersion": "2.0.0",
            "operations": [
                { "id": "sin", "label": "sin(x)", "inputs": 1 }
            ]
        }
    """.trimIndent()

        val result = parser.parse(manifestJson)

        assertTrue(result is ManifestParseResult.Success)
    }

    @Test
    fun `manifest with path traversal in id is rejected`() {
        val manifestJson = """
        {
            "id": "../../../../evil",
            "name": "Evil",
            "version": "1.0.0",
            "description": "Malicious plugin",
            "category": "MATH",
            "entryFile": "main.lua",
            "operations": [
                { "id": "x", "label": "x", "inputs": 1 }
            ]
        }
    """.trimIndent()

        val result = ManifestParser().parse(manifestJson)

        assertTrue(result is ManifestParseResult.Error)
        assertTrue((result as ManifestParseResult.Error).message.contains("Invalid plugin id"))
    }

    @Test
    fun `manifest with id equal to double dot is rejected`() {
        val manifestJson = """
        {
            "id": "..",
            "name": "Evil",
            "version": "1.0.0",
            "description": "Malicious plugin",
            "category": "MATH",
            "entryFile": "main.lua",
            "operations": [
                { "id": "x", "label": "x", "inputs": 1 }
            ]
        }
    """.trimIndent()

        val result = ManifestParser().parse(manifestJson)

        assertTrue(result is ManifestParseResult.Error)
    }

    @Test
    fun `manifest with id containing path separator is rejected`() {
        val manifestJson = """
        {
            "id": "com/example/evil",
            "name": "Evil",
            "version": "1.0.0",
            "description": "Malicious plugin",
            "category": "MATH",
            "entryFile": "main.lua",
            "operations": [
                { "id": "x", "label": "x", "inputs": 1 }
            ]
        }
    """.trimIndent()

        val result = ManifestParser().parse(manifestJson)

        assertTrue(result is ManifestParseResult.Error)
    }

    @Test
    fun `manifest with path traversal in entryFile is rejected`() {
        val manifestJson = """
        {
            "id": "com.example.evil",
            "name": "Evil",
            "version": "1.0.0",
            "description": "Malicious plugin",
            "category": "MATH",
            "entryFile": "../../../etc/passwd",
            "operations": [
                { "id": "x", "label": "x", "inputs": 1 }
            ]
        }
    """.trimIndent()

        val result = ManifestParser().parse(manifestJson)

        assertTrue(result is ManifestParseResult.Error)
        assertTrue((result as ManifestParseResult.Error).message.contains("entry file"))
    }

    @Test
    fun `manifest with absolute path entryFile is rejected`() {
        val manifestJson = """
        {
            "id": "com.example.evil",
            "name": "Evil",
            "version": "1.0.0",
            "description": "Malicious plugin",
            "category": "MATH",
            "entryFile": "/etc/passwd",
            "operations": [
                { "id": "x", "label": "x", "inputs": 1 }
            ]
        }
    """.trimIndent()

        val result = ManifestParser().parse(manifestJson)

        assertTrue(result is ManifestParseResult.Error)
    }

    @Test
    fun `manifest with normal nested entryFile is accepted`() {
        val manifestJson = """
        {
            "id": "com.example.nested",
            "name": "Nested",
            "version": "1.0.0",
            "description": "Plugin with nested entry file",
            "category": "MATH",
            "entryFile": "scripts/main.lua",
            "operations": [
                { "id": "x", "label": "x", "inputs": 1 }
            ]
        }
    """.trimIndent()

        val result = ManifestParser().parse(manifestJson)

        assertTrue(result is ManifestParseResult.Success)
    }

    @Test
    fun `manifest with malformed minAppVersion is rejected`() {
        val parser = ManifestParser(currentAppVersion = "1.0.0")
        val manifestJson = """
        {
            "id": "test-plugin",
            "name": "Test",
            "version": "1.0.0",
            "description": "Test plugin",
            "category": "MATH",
            "entryFile": "main.lua",
            "minAppVersion": "not-a-version",
            "operations": [
                { "id": "sin", "label": "sin(x)", "inputs": 1 }
            ]
        }
    """.trimIndent()

        val result = parser.parse(manifestJson)

        assertTrue(result is ManifestParseResult.Error)
    }

    @Test
    fun `manifest with duplicate operation ids is rejected`() {
        val manifestJson = """
            {
                "id": "com.example.dup",
                "name": "Dup",
                "version": "1.0.0",
                "description": "Plugin with clashing operation ids",
                "category": "MATH",
                "entryFile": "main.lua",
                "operations": [
                    { "id": "sin", "label": "sin(x)", "inputs": 1 },
                    { "id": "sin", "label": "sin again(x)", "inputs": 1 }
                ]
            }
        """.trimIndent()

        val result = ManifestParser().parse(manifestJson)

        assertTrue(result is ManifestParseResult.Error)
        assertTrue((result as ManifestParseResult.Error).message.contains("sin"))
    }

    @Test
    fun `manifest with unique operation ids is accepted`() {
        val manifestJson = """
            {
                "id": "com.example.unique",
                "name": "Unique",
                "version": "1.0.0",
                "description": "Plugin with distinct operation ids",
                "category": "MATH",
                "entryFile": "main.lua",
                "operations": [
                    { "id": "sin", "label": "sin(x)", "inputs": 1 },
                    { "id": "cos", "label": "cos(x)", "inputs": 1 }
                ]
            }
        """.trimIndent()

        val result = ManifestParser().parse(manifestJson)

        assertTrue(result is ManifestParseResult.Success)
        assertEquals(2, (result as ManifestParseResult.Success).plugin.operations.size)
    }
}
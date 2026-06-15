package com.github.mykolab31.plugincalculator.plugin

import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.data.model.PluginCategory
import com.github.mykolab31.plugincalculator.data.model.PluginOperation
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val category: String,
    val entryFile: String,
    val operations: List<PluginOperationDto>,
    val author: String = "Unknown",
    val minAppVersion: String = "1.0.0"
)

@Serializable
data class PluginOperationDto (
    val id: String,
    val label: String,
    val inputs: Int
)

sealed class ManifestParseResult {
    data class Success(val plugin: Plugin) : ManifestParseResult()
    data class Error(val message: String) : ManifestParseResult()
}

class ManifestParser {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Parses the JSON content of manifest.json into a Plugin domain model.
     * Unknown category strings fall back tp PluginCategory.OTHER.
     */
    fun parse(manifestJson: String): ManifestParseResult {
        val manifest = try {
            json.decodeFromString<PluginManifest>(manifestJson)
        } catch (e: SerializationException) {
            return ManifestParseResult.Error("Invalid manifest format: ${e.message}")
        } catch (e: Exception) {
            return ManifestParseResult.Error("Failed to parse manifest: ${e.message}")
        }

        if (manifest.operations.isEmpty()) {
            return ManifestParseResult.Error("Manifest must declare at least one operation")
        }

        val category = try {
            PluginCategory.valueOf(manifest.category.uppercase())
        } catch (e: IllegalArgumentException) {
            PluginCategory.OTHER
        }

        val plugin = Plugin(
            id = manifest.id,
            name = manifest.name,
            version = manifest.version,
            description = manifest.description,
            category = category,
            entryFile = manifest.entryFile,
            operations = manifest.operations.map {
                PluginOperation(id = it.id, label = it.label, inputs = it.inputs)
            }
        )

        return ManifestParseResult.Success(plugin)
    }

}
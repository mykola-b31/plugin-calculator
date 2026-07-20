package com.github.mykolab31.plugincalculator.plugin

import com.github.mykolab31.plugincalculator.BuildConfig
import com.github.mykolab31.plugincalculator.data.model.OperationArity
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.data.model.PluginCategory
import com.github.mykolab31.plugincalculator.data.model.PluginOperation
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val author: String = "Unknown",
    val version: String,
    val minAppVersion: String = "1.0.0",
    val description: String,
    val category: String,
    val entryFile: String,
    val operations: List<PluginOperationDto>
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

class ManifestParser(
    private val currentAppVersion: String = BuildConfig.VERSION_NAME
) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    companion object {
        // Plugin id becomes a directory name under filesDir/plugins/, so it must
        // never contain path separators or ".." - otherwise a malicious manifest
        // could write files outside the intended plugin directory
        private const val MAX_ID_LENGTH = 128
        private val ID_REGEX = Regex("[A-Za-z0-9._-]{1,$MAX_ID_LENGTH}")

        private fun isSafeRelativePath(path: String): Boolean {
            if (path.isBlank()) return false
            if (path.startsWith("/") || path.startsWith("\\")) return false
            val segments = path.split("/", "\\")
            return segments.none { it == ".." || it.isEmpty()}
        }
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

        if (!ID_REGEX.matches(manifest.id) || manifest.id == "." || manifest.id == "..") {
            return ManifestParseResult.Error(
                "Invalid plugin id '${manifest.id}': only letters, digits, dots, hyphens and " +
                        "underscores are allowed (max $MAX_ID_LENGTH characters), and it cannot be '.' or '..'"
            )
        }

        if (!isSafeRelativePath(manifest.entryFile)) {
            return ManifestParseResult.Error(
                "Invalid entry file '${manifest.entryFile}': must be relative path without '..' segments"
            )
        }

        if (manifest.operations.isEmpty()) {
            return ManifestParseResult.Error("Manifest must declare at least one operation")
        }

        val operations = manifest.operations.map { dto ->
            val arity = OperationArity.fromInputCount(dto.inputs)
                ?: return ManifestParseResult.Error(
                    "Operation '${dto.id}' declared inputs=${dto.inputs}, but only nullary, unary or binary is supported"
                )
            PluginOperation(id = dto.id, label = dto.label, arity = arity)
        }

        when (AppVersion.isAtLeast(currentAppVersion, manifest.minAppVersion)) {
            null -> return ManifestParseResult.Error(
                "Invalid minAppVersion format: '${manifest.minAppVersion}'"
            )
            false -> return ManifestParseResult.Error(
                "This plugin requires app version ${manifest.minAppVersion} or higher (current: $currentAppVersion)"
            )
            true -> Unit
        }

        val category = try {
            PluginCategory.valueOf(manifest.category.uppercase())
        } catch (_: IllegalArgumentException) {
            PluginCategory.OTHER
        }

        val plugin = Plugin(
            id = manifest.id,
            name = manifest.name,
            author = manifest.author,
            version = manifest.version,
            minAppVersion = manifest.minAppVersion,
            description = manifest.description,
            category = category,
            entryFile = manifest.entryFile,
            operations = operations
        )

        return ManifestParseResult.Success(plugin)
    }

}
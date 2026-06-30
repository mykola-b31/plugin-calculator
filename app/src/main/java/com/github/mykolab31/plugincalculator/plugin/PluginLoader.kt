package com.github.mykolab31.plugincalculator.plugin

import android.content.Context
import android.net.Uri
import com.github.mykolab31.plugincalculator.data.model.Plugin
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

sealed class PluginLoadResult {
    data class Success(val plugin: Plugin) : PluginLoadResult()
    data class Error(val message: String) : PluginLoadResult()
    data class AlreadyExists(val plugin: Plugin, val existingVersion: String) : PluginLoadResult()
}

class PluginLoader (
    private val context: Context,
    private val validator: PluginValidator = PluginValidator()
) {

    companion object {
        private const val MANIFEST_FILENAME = "manifest.json"
        private const val PLUGINS_DIR = "plugins"
        private const val MAX_ENTRY_SIZE_BYTES = 5 * 1024 * 1024L
    }

    /**
     * Loads and installs a .calcpkg file from the given Uri.
     * Extracts the archive, validates contents, and stores the plugin locally.
     */
    fun load(uri: Uri, overwrite: Boolean = false): PluginLoadResult {
        val extracted = try {
            extractArchive(uri)
        } catch (e: IOException) {
            return PluginLoadResult.Error("Failed to extract archive: ${e.message}")
        } catch (e: SecurityException) {
            return PluginLoadResult.Error("Security error while reading file: ${e.message}")
        }

        val manifestJson = extracted[MANIFEST_FILENAME]
            ?: return PluginLoadResult.Error("Archive does not contain manifest.json")

        val tempParseResult = ManifestParser().parse(manifestJson)
        if (tempParseResult is ManifestParseResult.Error) {
            return PluginLoadResult.Error("Manifest error: ${tempParseResult.message}")
        }
        val entryFile = (tempParseResult as ManifestParseResult.Success).plugin.entryFile

        val script = extracted[entryFile]
            ?: return PluginLoadResult.Error("Entry file '$entryFile' not found in archive")

        val validationResult = validator.validate(manifestJson, script)
        if (validationResult is ValidationResult.Invalid) {
            return PluginLoadResult.Error("Validation failed: ${validationResult.reason}")
        }

        val plugin = (validationResult as ValidationResult.Valid).plugin

        if (!overwrite) {
            val existingVersion = getInstalledVersion(plugin.id)
            if (existingVersion != null) {
                return PluginLoadResult.AlreadyExists(plugin, existingVersion)
            }
        }

        return try {
            savePlugin(plugin.id, extracted)
            PluginLoadResult.Success(plugin)
        } catch (e: IOException) {
            PluginLoadResult.Error("Failed to save plugin: ${e.message}")
        }
    }

    /**
     * Reads all entries from the ZIP archive into memory as strings.
     * Only text files (manifest, Lua scripts) are expected.
     */
    private fun extractArchive(uri: Uri): Map<String, String> {
        val entries = mutableMapOf<String, String>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val entryName = entry.name
                        if (entryName.contains("..") || entryName.startsWith("/")) {
                            throw SecurityException("Unsafe entry path detected: $entryName")
                        }

                        val content = zip.readNBytes((MAX_ENTRY_SIZE_BYTES + 1).toInt())
                        if (content.size > MAX_ENTRY_SIZE_BYTES) {
                            throw SecurityException(
                                "Entry '${entry.name}' exceeds maximum allowed size"
                            )
                        }
                        entries[entryName] = content.toString(Charsets.UTF_8)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: throw IOException("Could not open file stream")

        return entries
    }

    /**
     * Saves extracted plugin files to internal storage.
     * Path: filesDir/plugins/<pluginId>/
     */
    private fun savePlugin(pluginId: String, files: Map<String, String>) {
        val pluginDir = File(context.filesDir, "$PLUGINS_DIR/$pluginId")
        if (pluginDir.exists()) pluginDir.deleteRecursively()
        pluginDir.mkdirs()

        files.forEach { (name, content) ->
            File(pluginDir, name).writeText(content)
        }
    }

    /**
     * Returns the Lua script content for an installed plugin.
     * Used by PluginExecutor at calculation time.
     */
    fun readScript(plugin: Plugin): String? {
        val scriptFile = File(
            context.filesDir,
            "$PLUGINS_DIR/${plugin.id}/${plugin.entryFile}"
        )
        return if (scriptFile.exists()) scriptFile.readText() else null
    }

    /**
     * Removes a plugin from internal storage.
     */
    fun uninstall(pluginId: String): Boolean {
        val pluginDir = File(context.filesDir, "$PLUGINS_DIR/$pluginId")
        return pluginDir.deleteRecursively()
    }

    private fun getInstalledVersion(pluginId: String): String? {
        val manifestFile = File(context.filesDir, "$PLUGINS_DIR/$pluginId/$MANIFEST_FILENAME")
        if (!manifestFile.exists()) return null

        val manifestJson = manifestFile.readText()
        val result = ManifestParser().parse(manifestJson)
        return (result as? ManifestParseResult.Success)?.plugin?.version
    }
}
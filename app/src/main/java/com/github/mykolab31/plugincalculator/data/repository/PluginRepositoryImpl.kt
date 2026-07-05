package com.github.mykolab31.plugincalculator.data.repository

import android.content.Context
import android.net.Uri
import com.github.mykolab31.plugincalculator.data.model.CalculationResult
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.plugin.ManifestParseResult
import com.github.mykolab31.plugincalculator.plugin.ManifestParser
import com.github.mykolab31.plugincalculator.plugin.PluginExecutionResult
import com.github.mykolab31.plugincalculator.plugin.PluginExecutor
import com.github.mykolab31.plugincalculator.plugin.PluginLoadResult
import com.github.mykolab31.plugincalculator.plugin.PluginLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PluginRepositoryImpl(
    private val context: Context,
    private val loader: PluginLoader,
    private val executor: PluginExecutor,
    private val externalScope: CoroutineScope
) : PluginRepository {

    companion object {
        private const val PLUGINS_DIR = "plugins"
        private const val MANIFEST_FILENAME = "manifest.json"
        private const val ENABLED_PREFS = "plugin_enabled_states"
    }

    private val prefs by lazy {
        context.getSharedPreferences(ENABLED_PREFS, Context.MODE_PRIVATE)
    }

    private val _installPlugins = MutableStateFlow<List<Plugin>>(emptyList())
    override val installedPlugins: StateFlow<List<Plugin>> = _installPlugins.asStateFlow()

    init {
        externalScope.launch { refresh() }
    }

    override suspend fun refresh() {
        _installPlugins.value = readPluginsFromDisc()
    }

    private suspend fun readPluginsFromDisc(): List<Plugin> = withContext(Dispatchers.IO) {
        val pluginDir = File(context.filesDir, PLUGINS_DIR)
        if (!pluginDir.exists()) return@withContext emptyList()

        pluginDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                val manifestFile = File(dir, MANIFEST_FILENAME)
                if (!manifestFile.exists()) return@mapNotNull null

                val result = ManifestParser().parse(manifestFile.readText())
                val plugin = (result as? ManifestParseResult.Success)?.plugin
                    ?: return@mapNotNull null

                val isEnabled = prefs.getBoolean(plugin.id, true)
                plugin.copy(isEnabled = isEnabled)
            }
            ?: emptyList()
    }

    override suspend fun installPlugin(
        uri: Uri,
        overwrite: Boolean
    ): PluginLoadResult = withContext(Dispatchers.IO) {
        val result = loader.load(uri, overwrite)
        if (result is PluginLoadResult.Success) refresh()
        result
    }

    override suspend fun uninstallPlugin(pluginId: String): Boolean = withContext(Dispatchers.IO) {
        prefs.edit().remove(pluginId).apply()
        val removed = loader.uninstall(pluginId)
        if (removed) refresh()
        removed
    }

    override fun setPluginEnabled(pluginId: String, enabled: Boolean) {
        prefs.edit().putBoolean(pluginId, enabled).apply()
        _installPlugins.update { list ->
            list.map { plugin -> if (plugin.id == pluginId) plugin.copy(isEnabled = enabled) else plugin }
        }
    }

    override suspend fun executeOperation(
        plugin: Plugin,
        operationId: String,
        args: List<Double>
    ): CalculationResult = withContext(Dispatchers.IO) {
        val script = loader.readScript(plugin)
            ?: return@withContext CalculationResult.Err("Script not found for plugin '${plugin.id}'")

        when (val result = executor.execute(script, operationId, args)) {
            is PluginExecutionResult.Success -> result.result
            is PluginExecutionResult.Error -> CalculationResult.Err(result.message)
        }
    }

    override suspend fun getPluginById(pluginId: String): Plugin? = withContext(Dispatchers.IO) {
        val pluginDir = File(context.filesDir, "$PLUGINS_DIR/$pluginId")
        if (!pluginDir.exists() || !pluginDir.isDirectory) return@withContext null

        val manifestFile = File(pluginDir, MANIFEST_FILENAME)
        if (!manifestFile.exists()) return@withContext null

        val result = ManifestParser().parse(manifestFile.readText())
        val plugin = (result as? ManifestParseResult.Success)?.plugin ?: return@withContext null

        val isEnabled = prefs.getBoolean(plugin.id, true)
        plugin.copy(isEnabled = isEnabled)
    }
}
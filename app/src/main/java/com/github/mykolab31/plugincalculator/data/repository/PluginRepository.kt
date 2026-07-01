package com.github.mykolab31.plugincalculator.data.repository

import android.net.Uri
import com.github.mykolab31.plugincalculator.data.model.CalculationResult
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.plugin.PluginLoadResult

interface PluginRepository {

    /** Returns all installed plugins */
    suspend fun getInstalledPlugins(): List<Plugin>

    /** Loads and installs a plugin from a .calcpkg file */
    suspend fun installPlugin(uri: Uri, overwrite: Boolean = false): PluginLoadResult

    /** Removes a plugin by id */
    suspend fun uninstallPlugin(pluginId: String): Boolean

    /** Enables or disables a plugin */
    fun setPluginEnabled(pluginId: String, enabled: Boolean)

    /** Executes an operation from a specific plugin */
    suspend fun executeOperation(
        plugin: Plugin,
        operationId: String,
        args: List<Double>
    ): CalculationResult
}
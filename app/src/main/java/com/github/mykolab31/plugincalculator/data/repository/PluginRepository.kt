package com.github.mykolab31.plugincalculator.data.repository

import android.net.Uri
import com.github.mykolab31.plugincalculator.data.model.CalculationResult
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.plugin.PluginLoadResult

interface PluginRepository {

    /** Returns all installed plugins */
    fun getInstalledPlugins(): List<Plugin>

    /** Loads and installs a plugin from a .calcpkg file */
    fun installPlugin(uri: Uri, overwrite: Boolean = false): PluginLoadResult

    /** Removes a plugin by id */
    fun uninstallPlugin(pluginId: String): Boolean

    /** Enables or disables a plugin */
    fun setPluginEnabled(pluginId: String, enabled: Boolean)

    /** Executes an operation from a specific plugin */
    fun executeOperation(
        plugin: Plugin,
        operationId: String,
        args: List<Double>
    ): CalculationResult
}
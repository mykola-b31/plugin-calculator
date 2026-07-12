package com.github.mykolab31.plugincalculator.data.repository

import android.net.Uri
import com.github.mykolab31.plugincalculator.data.model.CalculationResult
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.plugin.PluginLoadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePluginRepository(
    initialPlugins: List<Plugin> = emptyList()
) : PluginRepository {

    private val _installedPlugins = MutableStateFlow(initialPlugins)
    override val installedPlugins: StateFlow<List<Plugin>> = _installedPlugins.asStateFlow()

    var executeOperationResult: CalculationResult = CalculationResult.Err("not stubbed")

    var lastExecuteArgs: List<Double>? = null
        private set

    fun setPlugins(plugins: List<Plugin>) {
        _installedPlugins.value = plugins
    }

    override suspend fun installPlugin(uri: Uri, overwrite: Boolean): PluginLoadResult {
        throw NotImplementedError("Not needed for CalculatorViewModel tests")
    }

    override suspend fun uninstallPlugin(pluginId: String): Boolean {
        throw NotImplementedError("Not needed for CalculatorViewModel tests")
    }

    override fun setPluginEnabled(pluginId: String, enabled: Boolean) {
        throw NotImplementedError("Not needed for CalculatorViewModel tests")
    }

    override suspend fun executeOperation(
        plugin: Plugin,
        operationId: String,
        args: List<Double>
    ): CalculationResult {
        lastExecuteArgs = args
        return executeOperationResult
    }

    override suspend fun refresh() { }
}
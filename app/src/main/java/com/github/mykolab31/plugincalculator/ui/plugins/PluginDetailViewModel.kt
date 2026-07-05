package com.github.mykolab31.plugincalculator.ui.plugins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.data.repository.PluginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PluginDetailUiState(
    val plugin: Plugin? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUninstalled: Boolean = false
)

class PluginDetailViewModel(
    private val repository: PluginRepository,
    private val pluginId: String
) : ViewModel() {

    private val _isUninstalled = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(PluginDetailUiState())
    val uiState: StateFlow<PluginDetailUiState> = combine(
        repository.installedPlugins,
        _isUninstalled,
        _error
    ) { plugins, isUninstalled, error ->
        val plugin = plugins.find { it.id == pluginId }
        PluginDetailUiState(
            plugin = plugin,
            isLoading = false,
            error = error ?: if (plugin == null && !isUninstalled) "Plugin not found" else null,
            isUninstalled = isUninstalled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PluginDetailUiState(isLoading = true)
    )

    fun uninstallPlugin() {
        viewModelScope.launch {
            val success = repository.uninstallPlugin(pluginId)

            if (success) _isUninstalled.value = true
            else _error.value = "Failed to uninstall plugin"
        }
    }

    class Factory(
        private val repository: PluginRepository,
        private val pluginId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PluginDetailViewModel(repository, pluginId) as T
        }
    }
}
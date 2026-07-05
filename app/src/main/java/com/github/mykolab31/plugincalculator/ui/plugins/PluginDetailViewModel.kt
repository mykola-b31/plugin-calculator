package com.github.mykolab31.plugincalculator.ui.plugins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.data.repository.PluginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _uiState = MutableStateFlow(PluginDetailUiState())
    val uiState: StateFlow<PluginDetailUiState> = _uiState.asStateFlow()

    init {
        loadPlugin()
    }

    private fun loadPlugin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val plugin = repository.getPluginById(pluginId)

            if (plugin != null) {
                _uiState.update { it.copy(plugin = plugin, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Plugin not found") }
            }
        }
    }

    fun uninstallPlugin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val isUninstalled = repository.uninstallPlugin(pluginId)

            if (isUninstalled) {
                _uiState.update { it.copy(isLoading = false, isUninstalled = true) }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to uninstall plugin")
                }
            }
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
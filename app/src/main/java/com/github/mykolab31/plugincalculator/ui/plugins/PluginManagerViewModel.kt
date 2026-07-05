package com.github.mykolab31.plugincalculator.ui.plugins

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.data.repository.PluginRepository
import com.github.mykolab31.plugincalculator.plugin.PluginLoadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PluginManagerUiState(
    val plugins: List<Plugin> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val installDialog: InstallDialogState? = null
)

data class InstallDialogState(
    val pendingUri: Uri,
    val incomingPlugin: Plugin,
    val existingVersion: String
)

sealed class PluginManagerEvent {
    data class InstallRequested(val uri: Uri) : PluginManagerEvent()
    data class InstallConfirmed(val uri: Uri) : PluginManagerEvent()
    data class UninstallRequested(val pluginId: String) : PluginManagerEvent()
    data class ToggleEnabled(val pluginId: String, val enabled: Boolean) : PluginManagerEvent()
    data object DismissDialog : PluginManagerEvent()
    data object DismissError : PluginManagerEvent()
}

class PluginManagerViewModel(
    private val repository: PluginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PluginManagerUiState(isLoading = true))
    val uiState: StateFlow<PluginManagerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.installedPlugins.collect { plugins ->
                _uiState.update { it.copy(plugins = plugins, isLoading = false) }
            }
        }
    }

    fun onEvent(event: PluginManagerEvent) {
        when (event) {
            is PluginManagerEvent.InstallRequested -> handleInstall(event.uri, overwrite = false)
            is PluginManagerEvent.InstallConfirmed -> handleInstall(event.uri, overwrite = true)
            is PluginManagerEvent.UninstallRequested -> handleUninstall(event.pluginId)
            is PluginManagerEvent.ToggleEnabled -> handleToggleEnabled(event.pluginId, event.enabled)
            is PluginManagerEvent.DismissDialog -> dismissDialog()
            is PluginManagerEvent.DismissError -> dismissError()
        }
    }

    private fun handleInstall(uri: Uri, overwrite: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, installDialog = null) }

            when (val result = repository.installPlugin(uri, overwrite)) {
                is PluginLoadResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is PluginLoadResult.AlreadyExists -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            installDialog = InstallDialogState(
                                pendingUri = uri,
                                incomingPlugin = result.plugin,
                                existingVersion = result.existingVersion
                            )
                        )
                    }
                }
                is PluginLoadResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    private fun handleUninstall(pluginId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val isUninstalled = repository.uninstallPlugin(pluginId)

            _uiState.update {
                if (isUninstalled) it.copy(isLoading = false)
                else it.copy(isLoading = false, error = "Failed to uninstall plugin. Please try again.")
            }
        }
    }

    private fun handleToggleEnabled(pluginId: String, enabled: Boolean) {
        repository.setPluginEnabled(pluginId, enabled)
    }

    private fun dismissDialog() {
        _uiState.update { it.copy(installDialog = null) }
    }

    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory (private val repository: PluginRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PluginManagerViewModel(repository) as T
        }
    }
}
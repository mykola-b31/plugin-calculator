package com.github.mykolab31.plugincalculator.ui.plugins

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.data.model.PluginCategory
import com.github.mykolab31.plugincalculator.data.repository.PluginRepository
import com.github.mykolab31.plugincalculator.ui.components.PluginItemCard

@Composable
fun PluginManagerScreen(
    repository: PluginRepository,
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PluginManagerViewModel = viewModel(
        factory = PluginManagerViewModel.Factory(repository)
    )
    val uiState by viewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.onEvent(PluginManagerEvent.InstallRequested(it))}
        }
    )

    PluginManagerScreenContent(
        uiState = uiState,
        onEvent = { viewModel.onEvent(it) },
        onNavigateToDetail = onNavigateToDetail,
        onNavigateBack = onNavigateBack,
        onInstallClick = {
            filePickerLauncher.launch(arrayOf("*/*"))
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginManagerScreenContent(
    uiState: PluginManagerUiState,
    onEvent: (PluginManagerEvent) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onInstallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Plugin Manager") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onInstallClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Install plugin")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when{
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.plugins.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = "There are no plugins installed. Press + to add.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.plugins, key = { it.id }) { plugin ->
                            PluginItemCard(
                                plugin = plugin,
                                onClick = { onNavigateToDetail(plugin.id) },
                                trailingContent = {
                                    Switch(
                                        checked = plugin.isEnabled,
                                        onCheckedChange = { isChecked ->
                                            onEvent(PluginManagerEvent.ToggleEnabled(plugin.id, isChecked))
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        uiState.installDialog?.let { dialogState ->
            AlertDialog(
                onDismissRequest = { onEvent(PluginManagerEvent.DismissDialog) },
                title = { Text("Update plugin?") },
                text = {
                    Text("The '${dialogState.incomingPlugin.name}' plugin already exists (version ${dialogState.existingVersion}). Want to overwrite it with version ${dialogState.incomingPlugin.version}?")
                },
                confirmButton = {
                    TextButton(onClick = { onEvent(PluginManagerEvent.InstallConfirmed(dialogState.pendingUri)) }) {
                        Text("Rewrite")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onEvent(PluginManagerEvent.DismissDialog)} ) {
                        Text("Cancel")
                    }
                }
            )
        }

        uiState.error?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { onEvent(PluginManagerEvent.DismissError) },
                title = { Text("Error") },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(onClick = { onEvent(PluginManagerEvent.DismissError)} ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PluginManagerScreenPreview() {
    val mocPlugins = listOf(
        Plugin(
            id = "math_advanced",
            name = "Advanced Math",
            author = "John Doe",
            version = "1.2.0",
            description = "Trigonometry and logarithms",
            category = PluginCategory.TRIGONOMETRY,
            entryFile = "",
            operations = emptyList(),
            isEnabled = true
        ),
        Plugin(
            id = "finance_tools",
            name = "Finance Tools",
            author = "Community",
            version = "0.9.5",
            description = "Currency and interest calculator",
            category = PluginCategory.OTHER,
            entryFile = "",
            operations = emptyList(),
            isEnabled = false
        )
    )

    val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colors) {
        PluginManagerScreenContent(
            uiState = PluginManagerUiState(plugins = mocPlugins),
            onEvent = {},
            onNavigateToDetail = {},
            onNavigateBack = {},
            onInstallClick = {}
        )
    }
}
package com.github.mykolab31.plugincalculator.ui.calculator

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mykolab31.plugincalculator.R
import com.github.mykolab31.plugincalculator.data.model.OperationArity
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.data.model.PluginCategory
import com.github.mykolab31.plugincalculator.data.model.PluginOperation
import com.github.mykolab31.plugincalculator.data.repository.PluginRepository
import com.github.mykolab31.plugincalculator.ui.components.ButtonType
import com.github.mykolab31.plugincalculator.ui.components.CalculatorButton
import com.github.mykolab31.plugincalculator.ui.components.IslandCard

@Composable
fun CalculatorScreen(
    repository: PluginRepository,
    onNavigateToPlugins: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CalculatorViewModel = viewModel(
        factory = CalculatorViewModel.Factory(repository)
    )
    val uiState by viewModel.uiState.collectAsState()

    CalculatorScreenContent(
        uiState = uiState,
        onEvent = { event -> viewModel.onEvent(event) },
        onNavigateToPlugins = onNavigateToPlugins,
        modifier = modifier
    )
}

@Composable
fun CalculatorScreenContent(
    uiState: CalculatorUiState,
    onEvent: (CalculatorEvent) -> Unit,
    onNavigateToPlugins: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header and navigation to plugins
            IslandCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.calculator_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    IconButton(onClick = onNavigateToPlugins) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_manage_plugins),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Calculator display
            IslandCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = uiState.error ?: uiState.result,
                        fontSize = 24.sp,
                        color = if (uiState.error != null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.expression.ifEmpty { "0" },
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Connected plugins operations
            if (uiState.plugins.isNotEmpty()) {
                PluginOperationsPanel(
                    plugins = uiState.plugins,
                    onOperationClick = { plugin, operationId ->
                        onEvent(
                            CalculatorEvent.PluginOperationPressed(
                                plugin,
                                operationId
                            )
                        )
                    }
                )
            }

            // Main keyboard
            IslandCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalculatorButton(
                            label = "AC",
                            type = ButtonType.ACTION,
                            onClick = { onEvent(CalculatorEvent.ClearPressed) },
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "⌫",
                            type = ButtonType.ACTION,
                            onClick = { onEvent(CalculatorEvent.BackspacePressed) },
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "+/-",
                            type = ButtonType.OPERATION,
                            onClick = { onEvent(CalculatorEvent.NegatePressed) },
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "÷",
                            type = ButtonType.OPERATION,
                            onClick = { onEvent(CalculatorEvent.OperationPressed("/")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalculatorButton(
                            label = "7",
                            type = ButtonType.NUMBER,
                            onClick = { onEvent(CalculatorEvent.NumberPressed("7")) },
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "8",
                            type = ButtonType.NUMBER,
                            onClick = { onEvent(CalculatorEvent.NumberPressed("8")) },
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "9",
                            type = ButtonType.NUMBER,
                            onClick = { onEvent(CalculatorEvent.NumberPressed("9")) },
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "×",
                            type = ButtonType.OPERATION,
                            onClick = { onEvent(CalculatorEvent.OperationPressed("*")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalculatorButton(
                            label = "4",
                            type = ButtonType.NUMBER,
                            onClick = { onEvent(CalculatorEvent.NumberPressed("4")) },
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "5",
                            type = ButtonType.NUMBER,
                            onClick = { onEvent(CalculatorEvent.NumberPressed("5")) },
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "6",
                            type = ButtonType.NUMBER,
                            onClick = { onEvent(CalculatorEvent.NumberPressed("6")) },
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "-",
                            type = ButtonType.OPERATION,
                            onClick = { onEvent(CalculatorEvent.OperationPressed("-")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalculatorButton(
                            label = "1",
                            type = ButtonType.NUMBER,
                            onClick = { onEvent(CalculatorEvent.NumberPressed("1"))},
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "2",
                            type = ButtonType.NUMBER,
                            onClick = { onEvent(CalculatorEvent.NumberPressed("2")) },
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "3",
                            type = ButtonType.NUMBER,
                            onClick = { onEvent(CalculatorEvent.NumberPressed("3")) },
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "+",
                            type = ButtonType.OPERATION,
                            onClick = { onEvent(CalculatorEvent.OperationPressed("+")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalculatorButton(
                            label = "0",
                            type = ButtonType.NUMBER,
                            isWide = true,
                            onClick = { onEvent(CalculatorEvent.NumberPressed("0")) },
                            modifier = Modifier.weight(2f).aspectRatio(2f)
                        )
                        CalculatorButton(
                            label = ".",
                            type = ButtonType.NUMBER,
                            onClick = { onEvent(CalculatorEvent.DecimalPressed) },
                            modifier = Modifier.weight(1f)
                        )
                        CalculatorButton(
                            label = "=",
                            type = ButtonType.OPERATION,
                            onClick = { onEvent(CalculatorEvent.EqualsPressed) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginOperationsPanel(
    plugins: List<Plugin>,
    onOperationClick: (Plugin, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPluginId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(plugins.map(Plugin::id)) {
        if (plugins.none { it.id == selectedPluginId }) {
            selectedPluginId = plugins.firstOrNull()?.id
        }
    }

    val selectedPlugin = plugins.firstOrNull { it.id == selectedPluginId }
        ?: plugins.firstOrNull()
        ?: return

    IslandCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 8.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(
                    items = plugins,
                    key = Plugin::id
                ) {
                    plugin ->
                    FilterChip(
                        selected = plugin.id == selectedPlugin.id,
                        onClick = { selectedPluginId = plugin.id },
                        label = {
                            Text(
                                text = plugin.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(
                    items = selectedPlugin.operations,
                    key = { operation -> "${selectedPlugin.id}:${operation.id}" }
                ) { operation ->
                    CalculatorButton(
                        label = operation.label,
                        type = ButtonType.PLUGIN,
                        isWide = true,
                        onClick = { onOperationClick(selectedPlugin, operation.id) },
                        modifier = Modifier
                            .width(80.dp)
                            .height(60.dp)
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, showSystemUi = true)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun CalculatorScreenPreview() {
    val mockState = CalculatorUiState(
        expression = "1024×2",
        result = "2048",
        plugins = listOf(
            Plugin(
                id = "math-utilities",
                name = "Math Utilities",
                author = "Demo",
                version = "1.0.0",
                minAppVersion = "1.0.0",
                description = "Common mathematical operations",
                category = PluginCategory.ARITHMETIC,
                entryFile = "main.lua",
                operations = listOf(
                    PluginOperation("square", "x²", OperationArity.UNARY),
                    PluginOperation("sqrt", "√x", OperationArity.UNARY),
                    PluginOperation("average", "avg", OperationArity.BINARY)
                )
            ),
            Plugin(
                id = "trigonometry",
                name = "Trigonometry",
                author = "Demo",
                version = "1.0.0",
                minAppVersion = "1.0.0",
                description = "Trigonometric operations",
                category = PluginCategory.TRIGONOMETRY,
                entryFile = "main.lua",
                operations = listOf(
                    PluginOperation("sin", "sin", OperationArity.UNARY),
                    PluginOperation("cos", "cos", OperationArity.UNARY),
                    PluginOperation("tan", "tan", OperationArity.UNARY)
                )
            )
        )
    )

    val useDarkTheme = isSystemInDarkTheme()

    val colors = if (useDarkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(colorScheme = colors) {
        CalculatorScreenContent(
            uiState = mockState,
            onEvent = {},
            onNavigateToPlugins = {}
        )
    }
}
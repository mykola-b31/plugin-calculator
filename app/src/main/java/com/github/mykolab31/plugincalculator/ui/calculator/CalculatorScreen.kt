package com.github.mykolab31.plugincalculator.ui.calculator

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.delay

private const val CALCULATION_INDICATOR_DELAY_MS = 200L

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
        onEvent = viewModel::onEvent,
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CalculatorHeader(
                onNavigateToPlugins = onNavigateToPlugins
            )

            CalculatorDisplay(
                uiState = uiState,
                modifier = Modifier.weight(1f)
            )

            if (uiState.plugins.isNotEmpty()) {
                PluginOperationsPanel(
                    plugins = uiState.plugins,
                    enabled = !uiState.isCalculating,
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

            CalculatorKeyboard(
                onEvent = onEvent,
                enabled = !uiState.isCalculating
            )
        }
    }
}

@Composable
private fun CalculatorHeader(
    onNavigateToPlugins: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.calculator_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalculatorDisplay(
    uiState: CalculatorUiState,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showCalculationIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isCalculating) {
        if (uiState.isCalculating) {
            delay(CALCULATION_INDICATOR_DELAY_MS)
            showCalculationIndicator = true
        } else {
            showCalculationIndicator = false
        }
    }

    val calculatingDescription = stringResource(
        R.string.cd_calculation_in_progress
    )
    val copiedMessage = stringResource(
        R.string.calculator_value_copied
    )
    val copyActionLabel = stringResource(
        R.string.cd_copy_calculator_value
    )
    val displayedExpression = uiState.expression
        .ifEmpty { "0" }
        .let { expression ->
            if (uiState.isExpressionApproximate) "≈ $expression" else expression
        }

    val copyValue = when {
        uiState.error != null -> null

        uiState.expression == "[matrix]" ->
            uiState.result.takeIf { it.isNotBlank() }

        else ->
            displayedExpression
    }

    IslandCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .combinedClickable(
                enabled = copyValue != null,
                onClick = {},
                onLongClickLabel = copyActionLabel,
                onLongClick = {
                    copyValue?.let { value ->
                        clipboardManager.setText(
                            AnnotatedString(value)
                        )

                        Toast.makeText(
                            context,
                            copiedMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ),
        elevation = 4.dp,
        cornerRadius = 20.dp,
        contentPadding = 20.dp
    ) {
        if (showCalculationIndicator) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(28.dp)
                    .semantics {
                        contentDescription = calculatingDescription
                    },
                strokeWidth = 3.dp
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            when {
                uiState.error != null -> {
                    Text(
                        text = uiState.error,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                uiState.result.contains('\n') -> {
                    Text(
                        text = uiState.result,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> {
                    AutoScrollingText(
                        text = uiState.result,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            AutoScrollingText(
                text = displayedExpression,
                fontSize = 48.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                scrollToStart = uiState.scrollExpressionToStart
            )
        }
    }
}

@Composable
private fun AutoScrollingText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    scrollToStart: Boolean = false
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(text, scrollState.maxValue, scrollToStart) {
        scrollState.scrollTo(
            if (scrollToStart) 0 else scrollState.maxValue
        )
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.horizontalScroll(scrollState)
        )
    }
}

@Composable
private fun PluginOperationsPanel(
    plugins: List<Plugin>,
    onOperationClick: (Plugin, String) -> Unit,
    enabled: Boolean,
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
        elevation = 3.dp,
        cornerRadius = 20.dp,
        contentPadding = 8.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(
                    items = plugins,
                    key = Plugin::id
                ) { plugin ->
                    FilterChip(
                        selected = plugin.id == selectedPlugin.id,
                        onClick = { selectedPluginId = plugin.id },
                        modifier = Modifier.widthIn(max = 180.dp),
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(
                    items = selectedPlugin.operations,
                    key = { operation -> "${selectedPlugin.id}:${operation.id}" }
                ) { operation ->
                    CalculatorButton(
                        label = operation.label,
                        type = ButtonType.PLUGIN,
                        enabled = enabled,
                        onClick = { onOperationClick(selectedPlugin, operation.id) },
                        modifier = Modifier
                            .width(80.dp)
                            .height(50.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatorKeyboard(
    onEvent: (CalculatorEvent) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorKey(
                label = "AC",
                type = ButtonType.ACTION,
                enabled = enabled,
                onClick = { onEvent(CalculatorEvent.ClearPressed) }
            )
            CalculatorKey(
                label = "⌫",
                type = ButtonType.ACTION,
                enabled = enabled,
                onClick = { onEvent(CalculatorEvent.BackspacePressed) }
            )
            CalculatorKey(
                label = "+/-",
                type = ButtonType.OPERATION,
                enabled = enabled,
                onClick = { onEvent(CalculatorEvent.NegatePressed) }
            )
            CalculatorKey(
                label = "÷",
                type = ButtonType.OPERATION,
                enabled = enabled,
                onClick = { onEvent(CalculatorEvent.OperationPressed("/")) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorKey("7", enabled = enabled) {
                onEvent(CalculatorEvent.NumberPressed("7"))
            }
            CalculatorKey("8", enabled = enabled) {
                onEvent(CalculatorEvent.NumberPressed("8"))
            }
            CalculatorKey("9", enabled = enabled) {
                onEvent(CalculatorEvent.NumberPressed("9"))
            }
            CalculatorKey(
                label = "×",
                type = ButtonType.OPERATION,
                enabled = enabled,
                onClick = { onEvent(CalculatorEvent.OperationPressed("*")) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorKey("4", enabled = enabled) {
                onEvent(CalculatorEvent.NumberPressed("4"))
            }
            CalculatorKey("5", enabled = enabled) {
                onEvent(CalculatorEvent.NumberPressed("5"))
            }
            CalculatorKey("6", enabled = enabled) {
                onEvent(CalculatorEvent.NumberPressed("6"))
            }
            CalculatorKey(
                label = "-",
                type = ButtonType.OPERATION,
                enabled = enabled,
                onClick = { onEvent(CalculatorEvent.OperationPressed("-")) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorKey("1", enabled = enabled) {
                onEvent(CalculatorEvent.NumberPressed("1"))
            }
            CalculatorKey("2", enabled = enabled) {
                onEvent(CalculatorEvent.NumberPressed("2"))
            }
            CalculatorKey("3", enabled = enabled) {
                onEvent(CalculatorEvent.NumberPressed("3"))
            }
            CalculatorKey(
                label = "+",
                type = ButtonType.OPERATION,
                enabled = enabled,
                onClick = { onEvent(CalculatorEvent.OperationPressed("+")) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorKey(
                label = "0",
                weight = 2f,
                enabled = enabled,
                onClick = { onEvent(CalculatorEvent.NumberPressed("0")) }
            )
            CalculatorKey(".", enabled = enabled) {
                onEvent(CalculatorEvent.DecimalPressed)
            }
            CalculatorKey(
                label = "=",
                type = ButtonType.OPERATION,
                enabled = enabled,
                onClick = { onEvent(CalculatorEvent.EqualsPressed) }
            )
        }
    }
}

@Composable
private fun RowScope.CalculatorKey(
    label: String,
    type: ButtonType = ButtonType.NUMBER,
    weight: Float = 1f,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    CalculatorButton(
        label = label,
        type = type,
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier
            .weight(weight)
            .height(58.dp)
    )
}

@Preview(
    name = "Light Mode",
    showBackground = true,
    showSystemUi = true
)
@Preview(
    name = "Compact Phone",
    showBackground = true,
    widthDp = 360,
    heightDp = 740
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun CalculatorScreenPreview() {
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
                    PluginOperation(
                        id = "square",
                        label = "x²",
                        arity = OperationArity.UNARY
                    ),
                    PluginOperation(
                        id = "sqrt",
                        label = "√x",
                        arity = OperationArity.UNARY
                    ),
                    PluginOperation(
                        id = "average",
                        label = "avg",
                        arity = OperationArity.BINARY
                    )
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
                    PluginOperation(
                        id = "sin",
                        label = "sin",
                        arity = OperationArity.UNARY
                    ),
                    PluginOperation(
                        id = "cos",
                        label = "cos",
                        arity = OperationArity.UNARY
                    ),
                    PluginOperation(
                        id = "tan",
                        label = "tan",
                        arity = OperationArity.UNARY
                    )
                )
            )
        )
    )

    val colors = if (isSystemInDarkTheme()) {
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
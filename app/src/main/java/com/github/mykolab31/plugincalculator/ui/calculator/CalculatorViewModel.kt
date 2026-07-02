package com.github.mykolab31.plugincalculator.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.mykolab31.plugincalculator.data.model.CalculationResult
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.data.repository.PluginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalculatorUiState(
    val expression: String = "",
    val result: String = "",
    val plugins: List<Plugin> = emptyList(),
    val isLoading: Boolean = false
)

sealed class CalculatorEvent {
    data class NumberPressed(val digit: String) : CalculatorEvent()
    data class OperationPressed(val symbol: String) : CalculatorEvent()
    data class PluginOperationPressed(
        val plugin: Plugin,
        val operationId: String
    ) : CalculatorEvent()
    data object EqualsPressed : CalculatorEvent()
    data object ClearPressed : CalculatorEvent()
    data object DecimalPressed : CalculatorEvent()
    data object NegatePressed : CalculatorEvent()
}

sealed class PendingOperation {
    data class BuiltIn (val symbol: String) : PendingOperation()
    data class PluginOp(val plugin: Plugin, val operationId: String, val label: String) : PendingOperation()
}

class CalculatorViewModel(
    private val repository: PluginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var firstOperand: Double? = null
    private var pendingOperation: PendingOperation? = null
    private var shouldResetExpression = false

    init {
        loadPlugins()
    }

    fun onEvent(event: CalculatorEvent) {
        when (event) {
            is CalculatorEvent.NumberPressed -> handleNumber(event.digit)
            is CalculatorEvent.OperationPressed -> handleOperation(event.symbol)
            is CalculatorEvent.PluginOperationPressed -> handlePluginOperation(
                event.plugin,
                event.operationId
            )
            is CalculatorEvent.EqualsPressed -> handleEquals()
            is CalculatorEvent.ClearPressed -> handleClear()
            is CalculatorEvent.DecimalPressed -> handleDecimal()
            is CalculatorEvent.NegatePressed -> handleNegate()
        }
    }

    private fun loadPlugins() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val plugins = repository.getInstalledPlugins().filter { it.isEnabled }
            _uiState.update { it.copy(plugins = plugins, isLoading = false) }
        }
    }

    private fun handleNumber(digit: String) {
        _uiState.update { state ->
            val currentExpression = if (shouldResetExpression) {
                shouldResetExpression = false
                digit
            } else {
                if (state.expression == "0") digit else state.expression + digit
            }
            state.copy(expression = currentExpression)
        }
    }

    private fun handleDecimal() {
        _uiState.update { state ->
            if (shouldResetExpression) {
                shouldResetExpression = false
                return@update state.copy(expression = "0.")
            }
            if (state.expression.contains(".")) return@update state

            state.copy(expression = state.expression + ".")
        }
    }

    private fun handleNegate() {
        _uiState.update { state ->
            val current = state.expression.toDoubleOrNull() ?: return@update state
            val negated = if (current % 1.0 == 0.0) {
                (-current).toInt().toString()
            } else {
                (-current).toString()
            }
            state.copy(expression = negated)
        }
    }

    private fun handleOperation(symbol: String) {
        val current = _uiState.value.expression.toDoubleOrNull() ?: return

        if (firstOperand != null && pendingOperation != null && !shouldResetExpression) {
            val op = pendingOperation
            if (op is PendingOperation.BuiltIn) {
                val intermediate = calculateBuiltIn(firstOperand!!, op.symbol, current)
                firstOperand = intermediate
                _uiState.update { it.copy(expression = formatResult(intermediate), result = "") }
            }
        } else {
            firstOperand = current
        }

        pendingOperation = PendingOperation.BuiltIn(symbol)
        shouldResetExpression = true
    }

    private fun handleEquals() {
        val current = _uiState.value.expression.toDoubleOrNull() ?: return
        val first = firstOperand ?: return
        val operation = pendingOperation ?: return

        shouldResetExpression = true

        when (operation) {
            is PendingOperation.PluginOp -> {
                val plugin = _uiState.value.plugins.find { it.id == operation.plugin.id } ?: return

                viewModelScope.launch {
                    val result = repository.executeOperation(plugin, operation.operationId, listOf(first, current))
                    handleCalculationResult(result, operation.label, current)

                    firstOperand = null
                    pendingOperation = null
                }
            }
            is PendingOperation.BuiltIn -> {
                val result = calculateBuiltIn(first, operation.symbol, current)
                _uiState.update { state ->
                    state.copy(
                        expression = formatResult(result),
                        result = "${formatResult(first)} ${operation.symbol} ${formatResult(current)}"
                    )
                }
                firstOperand = null
                pendingOperation = null
            }
        }
    }

    private fun handleClear() {
        firstOperand = null
        pendingOperation = null
        shouldResetExpression = false
        _uiState.update { CalculatorUiState(plugins = it.plugins) }
    }

    private fun handlePluginOperation(plugin: Plugin, operationId: String) {
        val operation = plugin.operations.find { it.id == operationId } ?: return
        val current = _uiState.value.expression.toDoubleOrNull() ?: return

        if (operation.inputs == 2) {
            firstOperand = current
            pendingOperation = PendingOperation.PluginOp(plugin, operationId, operation.label)
            shouldResetExpression = true
            return
        }

        shouldResetExpression = true

        viewModelScope.launch {
            val result = repository.executeOperation(plugin, operationId, listOf(current))
            handleCalculationResult(result, operation.label, current)
        }
    }

    private fun handleCalculationResult(
        result: CalculationResult,
        operationLabel: String,
        input: Double
    ) {
        when (result) {
            is CalculationResult.Number -> {
                shouldResetExpression = true
                _uiState.update { state ->
                    state.copy(
                        expression = formatResult(result.value),
                        result = "$operationLabel(${formatResult(input)}) = "
                    )
                }
            }
            is CalculationResult.Matrix -> {
                shouldResetExpression = true
                _uiState.update { state ->
                    state.copy(
                        expression = "[matrix]",
                        result = formatMatrix(result.rows)
                    )
                }
            }
            is CalculationResult.Err -> {
                shouldResetExpression = true
                _uiState.update { state ->
                    state.copy(result = "Error: ${result.message}")
                }
            }
        }
    }

    // built in operations (+, -, *, /)
    private fun calculateBuiltIn(a: Double, operation: String, b: Double): Double {
        return when(operation) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b != 0.0) a / b else Double.NaN
            else -> Double.NaN
        }
    }

    private fun formatResult(value: Double): String {
        return if (value % 1.0 == 0.0 && !value.isNaN() && !value.isInfinite()) {
            value.toInt().toString()
        } else {
            value.toString()
        }
    }

    private fun formatMatrix(rows: List<List<Double>>): String {
        return rows.joinToString(separator = "\n") { row ->
            row.joinToString(separator = "  ") { formatResult(it) }
        }
    }

    class Factory(private val repository: PluginRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalculatorViewModel(repository) as T
        }
    }
}
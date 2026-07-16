package com.github.mykolab31.plugincalculator.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.mykolab31.plugincalculator.data.model.CalculationResult
import com.github.mykolab31.plugincalculator.data.model.OperationArity
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.data.repository.PluginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

data class CalculatorUiState(
    val expression: String = "",
    val result: String = "",
    val error: String? = null,
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
    data object BackspacePressed : CalculatorEvent()
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

    companion object {
        private const val MAX_INPUT_LENGTH = 15
    }

    private val _uiState = MutableStateFlow(CalculatorUiState(isLoading = false))
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var firstOperand: BigDecimal? = null
    private var pendingOperation: PendingOperation? = null
    private var shouldResetExpression = false

    init {
        viewModelScope.launch {
            repository.installedPlugins.collect { plugins ->
                _uiState.update { it.copy(plugins = plugins.filter { p -> p.isEnabled }, isLoading = false) }
            }
        }
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
            is CalculatorEvent.BackspacePressed -> handleBackspace()
            is CalculatorEvent.DecimalPressed -> handleDecimal()
            is CalculatorEvent.NegatePressed -> handleNegate()
        }
    }

    private fun handleNumber(digit: String) {
        _uiState.update { state ->
            if (shouldResetExpression) {
                shouldResetExpression = false
                state.copy(expression = digit, error = null)
            } else {
                if (state.expression.length >= MAX_INPUT_LENGTH) return@update state

                val currentExpression = if (state.expression == "0") digit else state.expression + digit
                state.copy(expression = currentExpression, error = null)
            }
        }
    }

    private fun handleDecimal() {
        _uiState.update { state ->
            if (shouldResetExpression) {
                shouldResetExpression = false
                return@update state.copy(expression = "0.", error = null)
            }
            if (state.expression.contains(".")) return@update state

            state.copy(expression = state.expression + ".", error = null)
        }
    }

    private fun handleNegate() {
        _uiState.update { state ->
            val expr = state.expression
            if (expr == "0" || expr.isEmpty()) return@update state

            val negated = if (expr.startsWith("-")) expr.drop(1) else "-$expr"
            state.copy(expression = negated, error = null)
        }
    }

    private fun handleOperation(symbol: String) {
        processBinaryOperation(PendingOperation.BuiltIn(symbol))
    }

    private fun handleEquals() {
        val current = _uiState.value.expression.toBigDecimalOrNull() ?: return
        val first = firstOperand ?: return
        val operation = pendingOperation ?: return

        shouldResetExpression = true

        viewModelScope.launch {
            val finalResult = calculateIntermediateResult(first, operation, current)

            if (finalResult != null) {
                val opString = when (operation) {
                    is PendingOperation.BuiltIn -> operation.symbol
                    is PendingOperation.PluginOp -> operation.label
                }

                _uiState.update { state ->
                    state.copy(
                        expression = formatResult(finalResult),
                        result = "${formatResult(first)} $opString ${formatResult(current)} =",
                        error = null
                    )
                }
            }

            firstOperand = null
            pendingOperation = null
        }
    }

    private fun handleClear() {
        firstOperand = null
        pendingOperation = null
        shouldResetExpression = false
        _uiState.update { CalculatorUiState(plugins = it.plugins) }
    }

    private fun handleBackspace() {
        _uiState.update { state ->
            if (shouldResetExpression) return@update state

            val newExpr = state.expression.dropLast(1)
            val finalExpr = if (newExpr.isEmpty() || newExpr == "-") "0" else newExpr

            state.copy(expression = finalExpr, error = null)
        }
    }

    private fun handlePluginOperation(plugin: Plugin, operationId: String) {
        val operation = plugin.operations.find { it.id == operationId } ?: return

        when (operation.arity) {
            OperationArity.BINARY -> {
                processBinaryOperation(PendingOperation.PluginOp(plugin, operationId, operation.label))
            }

            OperationArity.UNARY -> {
                val current = _uiState.value.expression.toBigDecimalOrNull() ?: return
                shouldResetExpression = true
                viewModelScope.launch {
                    val result = repository.executeOperation(plugin, operationId, listOf(current))
                    handleCalculationResult(result, operation.label, current)
                }
            }

            OperationArity.NULLARY -> {
                shouldResetExpression = true
                viewModelScope.launch {
                    val result = repository.executeOperation(plugin, operationId, emptyList())
                    handleNullaryResult(result, operation.label)
                }
            }
        }
    }

    private fun handleCalculationResult(
        result: CalculationResult,
        operationLabel: String,
        input: BigDecimal
    ) {
        when (result) {
            is CalculationResult.Number -> {
                shouldResetExpression = true
                _uiState.update { state ->
                    state.copy(
                        expression = formatResult(result.value),
                        result = "$operationLabel(${formatResult(input)}) = ",
                        error = null
                    )
                }
            }
            is CalculationResult.Matrix -> {
                shouldResetExpression = true
                _uiState.update { state ->
                    state.copy(
                        expression = "[matrix]",
                        result = formatMatrix(result.rows),
                        error = null
                    )
                }
            }
            is CalculationResult.Err -> {
                shouldResetExpression = true
                _uiState.update { state ->
                    state.copy(error = result.message)
                }
            }
        }
    }

    private fun handleNullaryResult(result: CalculationResult, operationLabel: String) {
        when (result) {
            is CalculationResult.Number -> {
                _uiState.update { state ->
                    state.copy(
                        expression = formatResult(result.value),
                        result = "$operationLabel = ",
                        error = null
                    )
                }
            }
            is CalculationResult.Matrix -> {
                _uiState.update { state ->
                    state.copy(
                        expression = "[matrix]",
                        result = formatMatrix(result.rows),
                        error = null
                    )
                }
            }
            is CalculationResult.Err -> {
                _uiState.update { state ->
                    state.copy(error =  result.message)
                }
            }
        }
    }

    // built in operations (+, -, *, /)
    private fun calculateBuiltIn(a: BigDecimal, operation: String, b: BigDecimal): BigDecimal {
        return when(operation) {
            "+" -> a.add(b)
            "-" -> a.subtract(b)
            "*" -> a.multiply(b)
            "/" -> {
                if (b.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("Cannot divide by zero")
                a.divide(b, 10, RoundingMode.HALF_UP).stripTrailingZeros()
            }
            else -> throw IllegalArgumentException("Unknown operation")
        }
    }

    private fun processBinaryOperation(newOp: PendingOperation) {
        val current = _uiState.value.expression.toBigDecimalOrNull() ?: return

        if (firstOperand != null && pendingOperation != null && !shouldResetExpression) {
            viewModelScope.launch {
                val intermediate = calculateIntermediateResult(firstOperand!!, pendingOperation!!, current)

                if (intermediate != null) {
                    firstOperand = intermediate
                    pendingOperation = newOp
                    shouldResetExpression = true
                    val opString = when (newOp) {
                        is PendingOperation.BuiltIn -> newOp.symbol
                        is PendingOperation.PluginOp -> newOp.label
                    }

                    _uiState.update {
                        it.copy(
                            expression = formatResult(intermediate),
                            result = "${formatResult(intermediate)} $opString ",
                            error = null
                        )
                    }
                } else {
                    firstOperand = null
                    pendingOperation = null
                    shouldResetExpression = true
                }
            }
        } else {
            firstOperand = current
            pendingOperation = newOp
            shouldResetExpression = true

            val opString = when (newOp) {
                is PendingOperation.BuiltIn -> newOp.symbol
                is PendingOperation.PluginOp -> newOp.label
            }

            _uiState.update { state ->
                state.copy(result = "${formatResult(current)} $opString ", error = null)
            }
        }
    }

    private suspend fun calculateIntermediateResult(
        first: BigDecimal,
        operation: PendingOperation,
        current: BigDecimal
    ): BigDecimal? {
        return when (operation) {
            is PendingOperation.BuiltIn -> {
                try {
                    calculateBuiltIn(first, operation.symbol, current)
                } catch (_: ArithmeticException) {
                    _uiState.update { it.copy(error = builtInErrorMessage(operation.symbol, current)) }
                    null
                }
            }
            is PendingOperation.PluginOp -> {
                val plugin = _uiState.value.plugins.find { it.id == operation.plugin.id } ?: return null
                val result = repository.executeOperation(plugin, operation.operationId, listOf(first, current))
                when (result) {
                    is CalculationResult.Number -> result.value
                    is CalculationResult.Matrix -> {
                        _uiState.update { it.copy(error = "Cannot chain matrix operations") }
                        null
                    }
                    is CalculationResult.Err -> {
                        _uiState.update { it.copy(error = result.message) }
                        null
                    }
                }
            }
        }
    }

    private fun formatResult(value: BigDecimal): String {
        return value.stripTrailingZeros().toPlainString()
    }

    private fun builtInErrorMessage(symbol: String, divisor: BigDecimal): String {
        return if (symbol == "/" && divisor.compareTo(BigDecimal.ZERO) == 0) {
            "Cannot divide by zero"
        } else {
            "Result is undefined"
        }
    }

    private fun formatMatrix(rows: List<List<BigDecimal>>): String {
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
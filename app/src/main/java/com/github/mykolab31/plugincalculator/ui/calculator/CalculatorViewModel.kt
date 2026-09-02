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
    val isExpressionApproximate: Boolean = false,
    val scrollExpressionToStart: Boolean = false,
    val error: String? = null,
    val plugins: List<Plugin> = emptyList(),
    val isLoading: Boolean = false,
    val isCalculating: Boolean = false
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
        private const val SCIENTIFIC_NOTATION_MIN_EXPONENT = 15
    }

    private val _uiState = MutableStateFlow(CalculatorUiState(isLoading = false))
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var firstOperand: BigDecimal? = null
    private var firstOperandIsApproximate = false
    private var pendingOperation: PendingOperation? = null
    private var shouldResetExpression = false

    private fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    init {
        viewModelScope.launch {
            repository.installedPlugins.collect { plugins ->
                _uiState.update { it.copy(plugins = plugins.filter { p -> p.isEnabled }, isLoading = false) }
            }
        }
    }

    fun onEvent(event: CalculatorEvent) {
        if (_uiState.value.isCalculating) return

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
                state.copy(
                    expression = digit,
                    isExpressionApproximate = false,
                    scrollExpressionToStart = false,
                    error = null
                )
            } else {
                if (state.expression.length >= MAX_INPUT_LENGTH) return@update state

                val currentExpression = if (state.expression == "0") digit else state.expression + digit
                state.copy(
                    expression = currentExpression,
                    isExpressionApproximate = false,
                    scrollExpressionToStart = false,
                    error = null
                )
            }
        }
    }

    private fun handleDecimal() {
        _uiState.update { state ->
            if (shouldResetExpression) {
                shouldResetExpression = false
                return@update state.copy(
                    expression = "0.",
                    isExpressionApproximate = false,
                    scrollExpressionToStart = false,
                    error = null
                )
            }
            if (state.expression.contains(".")) return@update state

            state.copy(
                expression = state.expression + ".",
                isExpressionApproximate = false,
                scrollExpressionToStart = false,
                error = null
            )
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
        val currentState = _uiState.value
        val current = currentState.expression.toBigDecimalOrNull()
        if (current == null) {
            setError("Invalid number")
            return
        }
        val first = firstOperand ?: return
        val firstIsApproximate = firstOperandIsApproximate
        val operation = pendingOperation ?: return

        shouldResetExpression = true
        _uiState.update { it.copy(isCalculating = true) }

        viewModelScope.launch {
            try {
                val finalResult = calculateIntermediateResult(
                    first = first,
                    firstIsApproximate = firstIsApproximate,
                    operation = operation,
                    current = current,
                    currentIsApproximate = currentState.isExpressionApproximate)

                if (finalResult != null) {
                    val opString = when (operation) {
                        is PendingOperation.BuiltIn -> operation.symbol
                        is PendingOperation.PluginOp -> operation.label
                    }

                    _uiState.update { state ->
                        state.copy(
                            expression = formatResult(finalResult.value),
                            result = "${formatDisplayResult(first, firstIsApproximate)} $opString ${formatDisplayResult(current, currentState.isExpressionApproximate)} =",
                            isExpressionApproximate = finalResult.isApproximate,
                            scrollExpressionToStart = true,
                            error = null
                        )
                    }
                }

                firstOperand = null
                firstOperandIsApproximate = false
                pendingOperation = null
            } finally {
                _uiState.update { it.copy(isCalculating = false) }
            }
        }
    }

    private fun handleClear() {
        firstOperand = null
        firstOperandIsApproximate = false
        pendingOperation = null
        shouldResetExpression = false
        _uiState.update { CalculatorUiState(plugins = it.plugins) }
    }

    private fun handleBackspace() {
        _uiState.update { state ->
            if (shouldResetExpression) return@update state

            val newExpr = state.expression.dropLast(1)
            val finalExpr = if (newExpr.isEmpty() || newExpr == "-") "0" else newExpr

            state.copy(
                expression = finalExpr,
                isExpressionApproximate = false,
                scrollExpressionToStart = false,
                error = null
            )
        }
    }

    private fun handlePluginOperation(plugin: Plugin, operationId: String) {
        val operation = plugin.operations.find { it.id == operationId }
        if (operation == null) {
            setError("Operation '$operationId' not found in plugin '${plugin.name}'")
            return
        }

        when (operation.arity) {
            OperationArity.BINARY -> {
                processBinaryOperation(PendingOperation.PluginOp(plugin, operationId, operation.label))
            }

            OperationArity.UNARY -> {
                val currentState = _uiState.value
                val current = currentState.expression.toBigDecimalOrNull()
                if (current == null) {
                    setError("Invalid number")
                    return
                }
                shouldResetExpression = true
                _uiState.update { it.copy(isCalculating = true) }
                viewModelScope.launch {
                    try {
                        val result = repository.executeOperation(plugin, operationId, listOf(current))
                        handleCalculationResult(
                            result,
                            operation.label,
                            current,
                            inputIsApproximate = currentState.isExpressionApproximate)
                    } finally {
                        _uiState.update { it.copy(isCalculating = false) }
                    }
                }
            }

            OperationArity.NULLARY -> {
                shouldResetExpression = true
                _uiState.update { it.copy(isCalculating = true) }
                viewModelScope.launch {
                    try {
                        val result = repository.executeOperation(plugin, operationId, emptyList())
                        handleNullaryResult(result, operation.label)
                    } finally {
                        _uiState.update { it.copy(isCalculating = false) }
                    }
                }
            }
        }
    }

    private fun handleCalculationResult(
        result: CalculationResult,
        operationLabel: String,
        input: BigDecimal,
        inputIsApproximate: Boolean
    ) {
        when (result) {
            is CalculationResult.Number -> {
                shouldResetExpression = true
                _uiState.update { state ->
                    state.copy(
                        expression = formatResult(result.value),
                        result = "$operationLabel(${formatDisplayResult(input, inputIsApproximate)}) = ",
                        isExpressionApproximate = result.isApproximate || inputIsApproximate,
                        scrollExpressionToStart = true,
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
                        isExpressionApproximate = false,
                        scrollExpressionToStart = true,
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
                        isExpressionApproximate = result.isApproximate,
                        scrollExpressionToStart = true,
                        error = null
                    )
                }
            }
            is CalculationResult.Matrix -> {
                _uiState.update { state ->
                    state.copy(
                        expression = "[matrix]",
                        result = formatMatrix(result.rows),
                        isExpressionApproximate = false,
                        scrollExpressionToStart = true,
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
        val currentState = _uiState.value
        val current = currentState.expression.toBigDecimalOrNull()
        if (current == null) {
            setError("Invalid number")
            return
        }

        if (firstOperand != null && pendingOperation != null && !shouldResetExpression) {
            _uiState.update { it.copy(isCalculating = true) }

            viewModelScope.launch {
                try {
                    val intermediate = calculateIntermediateResult(
                        first = firstOperand!!,
                        firstIsApproximate = firstOperandIsApproximate,
                        operation = pendingOperation!!,
                        current = current,
                        currentIsApproximate = currentState.isExpressionApproximate)

                    if (intermediate != null) {
                        firstOperand = intermediate.value
                        firstOperandIsApproximate = intermediate.isApproximate
                        pendingOperation = newOp
                        shouldResetExpression = true
                        val opString = when (newOp) {
                            is PendingOperation.BuiltIn -> newOp.symbol
                            is PendingOperation.PluginOp -> newOp.label
                        }

                        _uiState.update {
                            it.copy(
                                expression = formatResult(intermediate.value),
                                result = "${formatDisplayResult(intermediate.value, intermediate.isApproximate)} $opString ",
                                isExpressionApproximate = intermediate.isApproximate,
                                scrollExpressionToStart = true,
                                error = null
                            )
                        }
                    } else {
                        firstOperand = null
                        firstOperandIsApproximate = false
                        pendingOperation = null
                        shouldResetExpression = true
                    }
                } finally {
                    _uiState.update { it.copy(isCalculating = false) }
                }
            }
        } else {
            firstOperand = current
            firstOperandIsApproximate = currentState.isExpressionApproximate
            pendingOperation = newOp
            shouldResetExpression = true

            val opString = when (newOp) {
                is PendingOperation.BuiltIn -> newOp.symbol
                is PendingOperation.PluginOp -> newOp.label
            }

            _uiState.update { state ->
                state.copy(result = "${formatDisplayResult(current, currentState.isExpressionApproximate)} $opString ", error = null)
            }
        }
    }

    private suspend fun calculateIntermediateResult(
        first: BigDecimal,
        firstIsApproximate: Boolean,
        operation: PendingOperation,
        current: BigDecimal,
        currentIsApproximate: Boolean
    ): CalculationResult.Number? {
        val hasApproximateInput = firstIsApproximate || currentIsApproximate

        return when (operation) {
            is PendingOperation.BuiltIn -> {
                try {
                    CalculationResult.Number(
                        value = calculateBuiltIn(first, operation.symbol, current),
                        isApproximate = hasApproximateInput
                    )
                } catch (_: ArithmeticException) {
                    _uiState.update { it.copy(error = builtInErrorMessage(operation.symbol, current)) }
                    null
                }
            }
            is PendingOperation.PluginOp -> {
                val plugin = _uiState.value.plugins.find { it.id == operation.plugin.id }
                if (plugin == null) {
                    _uiState.update { it.copy(error = "Plugin '${operation.label}' is no longer available") }
                    null
                } else {
                    val result = repository.executeOperation(
                        plugin,
                        operation.operationId,
                        listOf(first, current)
                    )
                    when (result) {
                        is CalculationResult.Number -> result.copy(isApproximate = result.isApproximate || hasApproximateInput)
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
    }

    private fun formatResult(value: BigDecimal): String {
        val normalized = value.stripTrailingZeros()
        val exponent = normalized.precision() - normalized.scale() - 1

        if (exponent >= SCIENTIFIC_NOTATION_MIN_EXPONENT) {
            val significand = normalized
                .movePointLeft(exponent)
                .stripTrailingZeros()
                .toPlainString()
            return "${significand}E+$exponent"
        }

        return normalized.toPlainString()
    }

    private fun formatDisplayResult(value: BigDecimal, isApproximate: Boolean): String {
        val formatted = formatResult(value)
        return if (isApproximate) "≈ $formatted" else formatted
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
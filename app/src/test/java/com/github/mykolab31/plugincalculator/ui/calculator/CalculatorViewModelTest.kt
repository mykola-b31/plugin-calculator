package com.github.mykolab31.plugincalculator.ui.calculator

import com.github.mykolab31.plugincalculator.MainDispatcherRule
import com.github.mykolab31.plugincalculator.data.model.CalculationResult
import com.github.mykolab31.plugincalculator.data.model.OperationArity
import com.github.mykolab31.plugincalculator.data.model.Plugin
import com.github.mykolab31.plugincalculator.data.model.PluginCategory
import com.github.mykolab31.plugincalculator.data.model.PluginOperation
import com.github.mykolab31.plugincalculator.data.repository.FakePluginRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CalculatorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakePluginRepository
    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setup() {
        repository = FakePluginRepository()
        viewModel = CalculatorViewModel(repository)
    }

    private fun testPlugin(
        operationId: String = "op",
        arity: OperationArity = OperationArity.UNARY
    ) = Plugin(
        id = "test-plugin",
        name = "Test Plugin",
        author = "test",
        version = "1.0.0",
        description = "test",
        category = PluginCategory.OTHER,
        entryFile = "main.lua",
        operations = listOf(PluginOperation(id = operationId, label = "op", arity = arity)),
        isEnabled = true
    )

    @Test
    fun `division by zero sets error field, not expression`() {
        viewModel.onEvent(CalculatorEvent.NumberPressed("5"))
        viewModel.onEvent(CalculatorEvent.OperationPressed("/"))
        viewModel.onEvent(CalculatorEvent.NumberPressed("0"))
        viewModel.onEvent(CalculatorEvent.EqualsPressed)

        val state = viewModel.uiState.value
        assertEquals("Division by zero", state.error)
    }

    @Test
    fun `valid built-in operation clears previous error`() {
        viewModel.onEvent(CalculatorEvent.NumberPressed("5"))
        viewModel.onEvent(CalculatorEvent.OperationPressed("/"))
        viewModel.onEvent(CalculatorEvent.NumberPressed("0"))
        viewModel.onEvent(CalculatorEvent.EqualsPressed)
        assertEquals("Division by zero", viewModel.uiState.value.error)

        viewModel.onEvent(CalculatorEvent.NumberPressed("4"))
        viewModel.onEvent(CalculatorEvent.OperationPressed("+"))
        viewModel.onEvent(CalculatorEvent.NumberPressed("6"))
        viewModel.onEvent(CalculatorEvent.EqualsPressed)

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals("10", state.expression)
    }

    @Test
    fun `pressing a number after an error clears the error`() {
        viewModel.onEvent(CalculatorEvent.NumberPressed("5"))
        viewModel.onEvent(CalculatorEvent.OperationPressed("/"))
        viewModel.onEvent(CalculatorEvent.NumberPressed("0"))
        viewModel.onEvent(CalculatorEvent.EqualsPressed)
        assertEquals("Division by zero", viewModel.uiState.value.error)

        viewModel.onEvent(CalculatorEvent.NumberPressed("7"))

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `clear resets error`() {
        viewModel.onEvent(CalculatorEvent.NumberPressed("5"))
        viewModel.onEvent(CalculatorEvent.OperationPressed("/"))
        viewModel.onEvent(CalculatorEvent.NumberPressed("0"))
        viewModel.onEvent(CalculatorEvent.EqualsPressed)
        assertEquals("Cannot divide by zero", viewModel.uiState.value.error)

        viewModel.onEvent(CalculatorEvent.ClearPressed)

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `plugin unary operation error sets error field`() {
        val plugin = testPlugin(arity = OperationArity.UNARY)
        repository.setPlugins(listOf(plugin))
        repository.executeOperationResult = CalculationResult.Err("Lua error: bad argument")

        viewModel.onEvent(CalculatorEvent.NumberPressed("9"))
        viewModel.onEvent(CalculatorEvent.PluginOperationPressed(plugin, "op"))

        val state = viewModel.uiState.value
        assertEquals("Lua error: bad argument", state.error)
        assertEquals(listOf(9.0), repository.lastExecuteArgs)
    }

    @Test
    fun `plugin unary operation success updates expression and clears error`() {
        val plugin = testPlugin(arity = OperationArity.UNARY)
        repository.setPlugins(listOf(plugin))
        repository.executeOperationResult = CalculationResult.Number(3.0)

        viewModel.onEvent(CalculatorEvent.NumberPressed("9"))
        viewModel.onEvent(CalculatorEvent.PluginOperationPressed(plugin, "op"))

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals("3", state.expression)
    }

    @Test
    fun `plugin nullary operation ignores current expression`() {
        val plugin = testPlugin(arity = OperationArity.NULLARY)
        repository.setPlugins(listOf(plugin))
        repository.executeOperationResult = CalculationResult.Number(3.14159)

        viewModel.onEvent(CalculatorEvent.PluginOperationPressed(plugin, "op"))

        val state = viewModel.uiState.value
        assertEquals("3.14159", state.expression)
        assertEquals(emptyList<Double>(), repository.lastExecuteArgs)
    }

    @Test
    fun `plugin binary operation error via equals sets error field`() {
        val plugin = testPlugin(arity = OperationArity.BINARY)
        repository.setPlugins(listOf(plugin))
        repository.executeOperationResult = CalculationResult.Err("division undefined")

        viewModel.onEvent(CalculatorEvent.NumberPressed("5"))
        viewModel.onEvent(CalculatorEvent.PluginOperationPressed(plugin, "op"))
        viewModel.onEvent(CalculatorEvent.NumberPressed("2"))
        viewModel.onEvent(CalculatorEvent.EqualsPressed)

        val state = viewModel.uiState.value
        assertEquals("division undefined", state.error)
        assertEquals(listOf(5.0, 2.0), repository.lastExecuteArgs)
    }
}
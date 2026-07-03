package com.github.mykolab31.plugincalculator.ui.calculator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.mykolab31.plugincalculator.data.repository.PluginRepository

@Composable
fun CalculatorScreen(
    repository: PluginRepository,
    onNavigateToPlugins : () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Here will be calculator")
    }
}
package com.github.mykolab31.plugincalculator.ui.plugins

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.mykolab31.plugincalculator.data.repository.PluginRepository

@Composable
fun PluginDetailScreen (
    pluginId: String,
    repository: PluginRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Details of plugin $pluginId")
    }
}
package com.github.mykolab31.plugincalculator.data.model

data class Plugin(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val category: PluginCategory,
    val entryFile: String,
    val operations: List<PluginOperation>,
    val isEnabled: Boolean = true
)

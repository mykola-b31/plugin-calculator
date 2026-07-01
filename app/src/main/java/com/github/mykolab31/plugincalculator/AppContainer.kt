package com.github.mykolab31.plugincalculator

import android.content.Context
import com.github.mykolab31.plugincalculator.data.repository.PluginRepository
import com.github.mykolab31.plugincalculator.data.repository.PluginRepositoryImpl

/**
 * Manual dependency container
 * Single instance lives in Application class.
 * Later can be replaced with Hilt module.
 */
class AppContainer(context: Context) {
    val pluginRepository: PluginRepository = PluginRepositoryImpl(context)
}
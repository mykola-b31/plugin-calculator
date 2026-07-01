package com.github.mykolab31.plugincalculator

import android.content.Context
import com.github.mykolab31.plugincalculator.data.repository.PluginRepository
import com.github.mykolab31.plugincalculator.data.repository.PluginRepositoryImpl
import com.github.mykolab31.plugincalculator.plugin.PluginExecutor
import com.github.mykolab31.plugincalculator.plugin.PluginLoader

/**
 * Manual dependency container
 * Single instance lives in Application class.
 * Later can be replaced with Hilt module.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val pluginExecutor: PluginExecutor by lazy {
        PluginExecutor()
    }

    val pluginLoader: PluginLoader by lazy {
        PluginLoader(appContext)
    }

    val pluginRepository: PluginRepository by lazy {
        PluginRepositoryImpl(
            context = appContext,
            loader = pluginLoader,
            executor = pluginExecutor
        )
    }
}
package com.github.mykolab31.plugincalculator

import android.app.Application

class PluginCalculatorApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
package com.github.mykolab31.plugincalculator.plugin

import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object LuaTimeoutRunner {

    fun runOrTimeoutMessage(timeoutSeconds: Long, block: () -> Unit): String? {
        val executor = Executors.newSingleThreadExecutor()
        return try {
            val future = executor.submit(block)
            try {
                future.get(timeoutSeconds, TimeUnit.SECONDS)
                null
            } catch (e: TimeoutException) {
                future.cancel(true)
                "execution exceeded ${timeoutSeconds}s - likely an infinite loop or excessive computation"
            } catch (e: ExecutionException) {
                "execution error: ${e.cause?.message ?: e.message}"
            }
        } finally {
            executor.shutdownNow()
        }
    }
}
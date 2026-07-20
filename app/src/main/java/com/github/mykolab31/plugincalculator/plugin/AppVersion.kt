package com.github.mykolab31.plugincalculator.plugin

object AppVersion {
    fun isAtLeast(version: String, minimum: String): Boolean? {
        val versionParts = parse(version) ?: return null
        val minimumParts = parse(minimum) ?: return null
        return compare(versionParts, minimumParts) >= 0
    }

    fun parse(version: String): List<Int>? {
        val segments = version.trim().split(".")
        if (segments.isEmpty()) return null
        return segments.map { it.toIntOrNull() ?: return null }
    }

    private fun compare(a: List<Int>, b: List<Int>): Int {
        val length = maxOf(a.size, b.size)
        for (i in 0 until length) {
            val diff = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }
}
package com.github.mykolab31.plugincalculator.data.model

sealed class CalculationResult {
    data class Number(val value: Double) : CalculationResult()
    data class Matrix(val rows: List<List<Double>>) : CalculationResult()
    data class Err(val message: String) : CalculationResult()
}
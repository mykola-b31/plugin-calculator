package com.github.mykolab31.plugincalculator.data.model

import java.math.BigDecimal

sealed class CalculationResult {
    data class Number(val value: BigDecimal, val isApproximate: Boolean = false) : CalculationResult()
    data class Matrix(val rows: List<List<BigDecimal>>) : CalculationResult()
    data class Err(val message: String) : CalculationResult()
}
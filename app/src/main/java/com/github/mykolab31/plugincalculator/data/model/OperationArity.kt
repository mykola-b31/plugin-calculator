package com.github.mykolab31.plugincalculator.data.model

enum class OperationArity(val inputCount: Int) {
    NULLARY(0),
    UNARY(1),
    BINARY(2);

    companion object {
        fun fromInputCount(count: Int): OperationArity? =
            entries.find { it.inputCount == count }
    }
}
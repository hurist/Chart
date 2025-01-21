package com.hvi2ist.chartlib

enum class PosType(val value: Int) {
    TOP(0), // 最上方
    ABOVE(1), // 上方
    ;

    companion object {
        fun fromValue(value: Int): PosType {
            return when (value) {
                0 -> TOP
                1 -> ABOVE
                else -> throw IllegalArgumentException("Unknown value: $value")
            }
        }
    }
}
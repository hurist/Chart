package com.hvi2ist.chartlib.util

import kotlin.math.max

object ChartUtil {

    /**
     * 主要是用于计算图表的最大值，如果有targetValue，那么maxValue和targetValue中的最大值作为基准
     * 要确保得出得新的最大值能被4整除
     */
    fun getChartMaxValue(maxValue: Int, targetValue: Int = -1): Int {
        val max = max(maxValue, targetValue)
        val value = listOf(10, 20, 50, 100, 200, 300, 400, 500, 600, 700, 800, 1000, 2000, 5000, 10000, 20000, 50000, 100000)
        val t =  value.find {
            it * 4 > max
        } ?: 1000000
        return t * 4
    }
}
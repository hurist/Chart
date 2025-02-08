package com.hvi2ist.chartlib.util

import kotlin.math.max

object ChartUtil {

    /**
     * 主要是用于计算图表的最大值，如果有targetValue，那么maxValue和targetValue中的最大值作为基准
     * 要确保得出得新的最大值能被4整除
     */
    fun getChartMaxValue(maxValue: Float, targetValue: Float = -1f): Long {
        val max = max(maxValue, targetValue)
        val value = listOf(10, 20, 25, 30, 40, 50, 60, 80, 100, 200, 300, 400, 500, 600, 700, 800, 1000, 2000, 2500, 3000, 4500, 5000, 10000, 20000, 50000, 100000, 1000000, 10000000, 1000000000, 1000000000)
        val t =  value.find {
            it * 4 > max
        } ?: 1000000
        return t * 4L
    }
}
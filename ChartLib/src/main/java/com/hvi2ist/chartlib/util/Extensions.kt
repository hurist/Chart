package com.hvi2ist.chartlib.util

import android.graphics.Color

/**
 * 扩展函数：更改颜色的透明度
 * @param alpha 透明度值，范围从 0（完全透明）到 255（完全不透明）
 * @return 带有新透明度的颜色值
 * @throws IllegalArgumentException 如果输入的 Int 不是有效的颜色值
 */
fun Int.changeAlpha(alpha: Int): Int {
    // 检查是否为有效颜色值
    require(isValidColor()) { "The Int value is not a valid color." }
    require(alpha in 0..255) { "Alpha must be between 0 and 255." }

    return Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))
}

/**
 * 检查 Int 是否为有效颜色
 * @return Boolean 是否是有效颜色
 */
private fun Int.isValidColor(): Boolean {
    return try {
        // 尝试解析颜色中的 Red、Green、Blue 值
        Color.red(this)
        Color.green(this)
        Color.blue(this)
        true // 如果没有异常，说明是有效颜色
    } catch (e: IllegalArgumentException) {
        false // 如果抛出异常，说明不是有效颜色
    }
}
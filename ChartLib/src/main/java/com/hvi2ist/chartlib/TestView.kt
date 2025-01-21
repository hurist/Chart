package com.hvi2ist.chartlib

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class TestView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = RectF(0f, 0f, 100f, 100f)
        canvas.drawRoundRect(rect, 50f, 50f, Paint().apply {
            isAntiAlias = true
            color = 0xff0000ff.toInt()
            style = Paint.Style.FILL
        })
        canvas.drawRect(Rect(0, 0, 100, 100), Paint().apply {
            isAntiAlias = true
            color = 0xff00ff00.toInt()
            style = Paint.Style.STROKE
        })
    }
}
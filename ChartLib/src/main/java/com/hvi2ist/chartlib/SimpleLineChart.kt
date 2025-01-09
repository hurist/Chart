package com.hvi2ist.chartlib

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.view.marginEnd
import com.hvi2ist.chartlib.util.changeAlpha
import com.hvi2ist.chartlib.util.dp
import com.hvi2ist.chartlib.util.sp

class SimpleLineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data = listOf(
        ChartData("--", ChartData.NO_VALUE),
        ChartData("--", ChartData.NO_VALUE),
        ChartData("--", ChartData.NO_VALUE)
    )
    private var focusedIndex = -1

    private val xAxisTextSize = 12.sp
    private val xAxisTextMargin = 6.dp
    private val xAxisTextColor = Color.BLACK
    private val xAxisTextPaint = Paint().apply {
        color = xAxisTextColor
        textSize = xAxisTextSize
        textAlign = Paint.Align.CENTER
    }
    private val xAxisLineWidth = 1.dp
    private val xAxisLinePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = xAxisLineWidth
    }

    private val dotRadius = 4.dp
    private val focusedDotRadius = 10.dp
    private val themeColor = Color.GREEN
    private val lineWidth = 1.dp

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val textHeight = xAxisTextPaint.fontMetrics.descent - xAxisTextPaint.fontMetrics.ascent
        val xAxisTextY = height.toFloat()
        data.map { it.time }.forEachIndexed { index, text ->
            val textX = when (index) {
                0 -> 0
                data.size - 1 -> width.toFloat()
                else -> width / (data.size - 1) * index.toFloat()
            }.toFloat()
            xAxisTextPaint.textAlign = when (index) {
                0 -> Paint.Align.LEFT
                data.size - 1 -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            canvas.drawText(text, textX, xAxisTextY, xAxisTextPaint)
        }

        val xAxisLineY = xAxisTextY - textHeight - xAxisTextMargin
        canvas.drawLine(0f, xAxisLineY, width.toFloat(), xAxisLineY, xAxisLinePaint)

        if (data.all { it.value == ChartData.NO_VALUE }) return

        val yAxis = data.map { it.value }
        val max = yAxis.maxOrNull() ?: 0
        val yUnit = (xAxisLineY) / max
        val path = Path()
        val range = Path()
        val dotPos = mutableListOf<Triple<Float, Float, Float>>()
        yAxis.forEachIndexed { index, yValue ->
            val radius = if (index != focusedIndex) dotRadius else focusedDotRadius - 4.dp
            var dotX = width / (yAxis.size - 1) * index.toFloat()
            if (index == 0) dotX = radius
            if (index == data.size - 1) dotX = width.toFloat() - radius

            val dotY = (xAxisLineY - yValue * yUnit).coerceIn(radius, xAxisLineY)
            dotPos.add(Triple(dotX, dotY, radius))

            if (index == 0) {
                path.moveTo(dotX, dotY)
                range.moveTo(dotX, dotY)
            } else {
                path.lineTo(dotX, dotY)
                range.lineTo(dotX, dotY)
            }
        }
        canvas.drawPath(path, Paint().apply {
            color = themeColor
            style = Paint.Style.STROKE
            strokeWidth = lineWidth
        })
        range.lineTo(dotPos.last().first, xAxisLineY)
        range.lineTo(dotPos.first().first, xAxisLineY)
        range.close()
        canvas.drawPath(range, Paint().apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, 0f, 0f, xAxisLineY,
                themeColor.changeAlpha(80), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
        })

        dotPos.forEachIndexed { index, (dotX, dotY, radius) ->
            if (index == focusedIndex) {
                canvas.drawCircle(dotX, dotY, focusedDotRadius, Paint().apply {
                    style = Paint.Style.FILL
                    shader = RadialGradient(
                        dotX, dotY, focusedDotRadius,
                        themeColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
                    )
                })
                canvas.drawCircle(dotX, dotY, focusedDotRadius - 2.dp, Paint().apply {
                    style = Paint.Style.FILL
                    color = Color.WHITE
                })
            }
            canvas.drawCircle(dotX, dotY, radius, Paint().apply {
                color = themeColor
                style = Paint.Style.FILL
            })
        }
    }

    fun setData(data: List<ChartData>, focusedIndex: Int = data.indexOfLast { it.value != ChartData.NO_VALUE }) {
        this.data = data
        if (data.first().value == ChartData.NO_VALUE) {
            this.data = this.data.mapIndexed { index, chartData ->
                if (index == 0) chartData.copy(value = 0)
                else chartData
            }
        }
        this.focusedIndex = focusedIndex
        invalidate()
    }

    companion object {
        private const val TAG = "SimpleLineChart"

        data class ChartData(val time: String, val value: Int = NO_VALUE) {
            companion object {
                const val NO_VALUE = -1
            }
        }
    }
}
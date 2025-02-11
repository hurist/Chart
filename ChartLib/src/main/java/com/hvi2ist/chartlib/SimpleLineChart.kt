package com.hvi2ist.chartlib

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
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

    private var axisLineColor = Color.BLACK
    private var axisTextSize = 12.sp
    private var axisTextMargin = 6.dp
    private val axisTextColor = Color.BLACK
    private val axisTextPaint = Paint().apply {
        color = axisTextColor
        textSize = axisTextSize
        textAlign = Paint.Align.CENTER
    }
    private var axisLineWidth = 1.dp
    private val axisLinePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = axisLineWidth
    }

    private var dotRadius = 4.dp
    private var focusedDotRadius = 10.dp
    private var themeColor = Color.GREEN
    private val lineWidth = 1.dp
    private var startColor = Color.GREEN
    private var endColor = Color.TRANSPARENT
    private var showPlaceholder = false

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SimpleLineChart, defStyleAttr, 0)
        axisLineColor = typedArray.getColor(R.styleable.SimpleLineChart_axisLineColor, axisLineColor)
        axisTextSize = typedArray.getDimension(R.styleable.SimpleLineChart_axisTextSize, axisTextSize)
        axisTextMargin = typedArray.getDimension(R.styleable.SimpleLineChart_chartBottomMargin, axisTextMargin)
        axisLineWidth = typedArray.getDimension(R.styleable.SimpleLineChart_axisLineWidth, axisLineWidth)
        axisTextPaint.textSize = axisTextSize
        axisLinePaint.color = axisLineColor
        axisLinePaint.strokeWidth = axisLineWidth
        dotRadius = typedArray.getDimension(R.styleable.SimpleLineChart_dotRadius, dotRadius)
        focusedDotRadius = typedArray.getDimension(R.styleable.SimpleLineChart_dotSelectedRadius, focusedDotRadius)
        themeColor = typedArray.getColor(R.styleable.SimpleLineChart_color, themeColor)
        startColor = typedArray.getColor(R.styleable.SimpleLineChart_startColor, startColor)
        endColor = typedArray.getColor(R.styleable.SimpleLineChart_endColor, endColor)
        showPlaceholder = typedArray.getBoolean(R.styleable.SimpleLineChart_showPlaceHolder, showPlaceholder)
        typedArray.recycle()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val textHeight = axisTextPaint.fontMetrics.descent - axisTextPaint.fontMetrics.ascent
        val xAxisTextY = height.toFloat()
        data.map { it.time }.forEachIndexed { index, text ->
            val textX = when (index) {
                0 -> 0
                data.size - 1 -> width.toFloat()
                else -> width / (data.size - 1) * index.toFloat()
            }.toFloat()
            axisTextPaint.textAlign = when (index) {
                0 -> Paint.Align.LEFT
                data.size - 1 -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            canvas.drawText(text, textX, xAxisTextY, axisTextPaint)
        }

        val xAxisLineY = xAxisTextY - textHeight - axisTextMargin
        canvas.drawLine(0f, xAxisLineY, width.toFloat(), xAxisLineY, axisLinePaint)

        //if (data.all { it.value == ChartData.NO_VALUE }) return

        val yAxis = data.map { it.value }
        val max = yAxis.maxOrNull() ?: 0
        val yUnit = (xAxisLineY) / max
        val path = Path()
        val range = Path()
        val dotPos = mutableListOf<Triple<Float, Float, Float>>()
        val firstValidIndex = data.indexOfFirst { it.value != ChartData.NO_VALUE }
        val lastValidIndex = data.indexOfLast { it.value != ChartData.NO_VALUE }

        // 没有有效数据
        if (lastValidIndex == -1 && firstValidIndex == -1) {
            if (showPlaceholder) {
                range.moveTo(dotRadius, dotRadius)
                range.lineTo(width.toFloat() - dotRadius, dotRadius)
                range.lineTo(width.toFloat() - dotRadius, xAxisLineY)
                range.lineTo(dotRadius, xAxisLineY)
                range.close()
                canvas.drawPath(range, Paint().apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        0f, 0f, 0f, xAxisLineY,
                        startColor, endColor, Shader.TileMode.CLAMP
                    )
                })
            }
            return
        }

        yAxis.forEachIndexed { index, yValue ->
            val lastValidPoint = dotPos.lastOrNull { it.second != INVALID_Y }
            val radius = if (index != focusedIndex) dotRadius else focusedDotRadius - 4.dp
            var dotX = width / (yAxis.size - 1) * index.toFloat()
            if (index == 0) dotX = radius
            if (index == data.size - 1) dotX = width.toFloat() - radius

            val dotY = if (yValue == ChartData.NO_VALUE) {
                INVALID_Y
            } else {
                (xAxisLineY - yValue * yUnit).coerceIn(radius, xAxisLineY)
            }
            dotPos.add(Triple(dotX, dotY, radius))

            if (dotY != INVALID_Y) {
                if (index == firstValidIndex) {
                    path.moveTo(dotX, dotY)
                    range.moveTo(dotX, dotY)
                } else {
                    path.lineTo(dotX, dotY)
                    range.lineTo(dotX, dotY)
                }
            }
        }
        canvas.drawPath(path, Paint().apply {
            color = themeColor
            style = Paint.Style.STROKE
            strokeWidth = lineWidth
        })

        if (firstValidIndex != lastValidIndex) {
            val firstX = dotPos[firstValidIndex].first
            val lastX = dotPos[lastValidIndex].first
            range.lineTo(lastX, xAxisLineY)
            range.lineTo(firstX, xAxisLineY)
            range.close()
            canvas.drawPath(range, Paint().apply {
                style = Paint.Style.FILL
                shader = LinearGradient(
                    0f, 0f, 0f, xAxisLineY,
                    startColor, endColor, Shader.TileMode.CLAMP
                )
            })
        }

        dotPos.forEachIndexed { index, (dotX, dotY, radius) ->
            if (dotY == INVALID_Y) return@forEachIndexed
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

    fun setData(
        data: List<ChartData>,
        focusedIndex: Int = data.indexOfLast { it.value != ChartData.NO_VALUE }
    ) {
        this.data = data
        /*if (data.first().value == ChartData.NO_VALUE) {
            this.data = this.data.mapIndexed { index, chartData ->
                if (index == 0) chartData.copy(value = 0)
                else chartData
            }
        }*/
        this.focusedIndex = focusedIndex
        invalidate()
    }

    companion object {
        private const val TAG = "SimpleLineChart"
        private const val INVALID_Y = -100f
    }

    data class ChartData(val time: String, val value: Int = NO_VALUE) {
        companion object {
            const val NO_VALUE = -1
        }
    }
}
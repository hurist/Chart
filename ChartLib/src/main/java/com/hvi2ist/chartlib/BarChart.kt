package com.hvi2ist.chartlib

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.hvi2ist.chartlib.util.dp
import com.hvi2ist.chartlib.util.sp

class BarChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetValue = -1
    private var maxValue = 2000
    private var data = listOf<BarData>()
    private var valueUnit = "ml"

    private var chartStartX = 0f
    private var chartBottomY = 0f
    private var touchedBarIndex = -1
    private var barRanges = mutableListOf<BarRange>()


    // 表本体的高度，不包含单位那部分
    private var chartHeight = 200.dp

    // 横轴到底部文字的距离
    private var chartBottomMargin = 10.dp

    // 纵轴到左边文字的距离
    private var chartLeftMargin = 10.dp

    // 图标到view最顶部的距离， valueUnitText和触摸信息会在这个空间内绘制
    private var chartTopMargin = 60.dp

    // 横纵轴文字和单位文字的颜色
    private var axisTextColor = Color.BLACK

    // 横纵轴文字的大小
    private var axisTextSize = 12.sp
    private var axisLineColor = Color.BLACK
    private var axisLineWidth = 4.dp
    private var valueUnitTextSize = 12.sp
    private var valueUnitText = "Unit(ml/hours)"

    // 未到达目标值的颜色
    private var barUnreachedColor = Color.GRAY
    private var barColor = Color.BLUE
    private var barMaxWidth = 18.dp
    private var barMinWidth = 2.dp
    private var targetLineColor = Color.RED

    // 柱状数据最小间隔
    private var barMinSpace = 2.dp

    private var touchLineColor = Color.RED
    private var touchInfoInnerVerticalPadding = 10.dp
    private var touchInfoInnerHorizontalPadding = 12.dp
    private var touchInfoCorner = 4.dp
    private var touchInfoBackgroundColor = Color.GRAY
    private var touchInfoTextSize = 12.sp
    private var touchInfoTextColor = Color.BLACK

    private lateinit var textPaint: Paint
    private lateinit var valueUnitTextPaint: Paint
    private lateinit var axisLinePaint: Paint
    private lateinit var barPaint: Paint
    private lateinit var touchInfoTextPaint: Paint
    private lateinit var touchLinePaint: Paint
    private lateinit var targetLinePaint: Paint
    private lateinit var touchInfoBackgroundPaint: Paint

    init {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.BarChart, defStyleAttr, 0)

        chartHeight = typedArray.getDimension(R.styleable.BarChart_chartHeight, chartHeight)
        chartBottomMargin =
            typedArray.getDimension(R.styleable.BarChart_chartBottomMargin, chartBottomMargin)
        chartLeftMargin =
            typedArray.getDimension(R.styleable.BarChart_chartLeftMargin, chartLeftMargin)
        chartTopMargin =
            typedArray.getDimension(R.styleable.BarChart_chartTopMargin, chartTopMargin)
        axisTextColor = typedArray.getColor(R.styleable.BarChart_axisTextColor, axisTextColor)
        axisTextSize = typedArray.getDimension(R.styleable.BarChart_axisTextSize, axisTextSize)
        axisLineColor = typedArray.getColor(R.styleable.BarChart_axisLineColor, axisLineColor)
        axisLineWidth = typedArray.getDimension(R.styleable.BarChart_axisLineWidth, axisLineWidth)
        valueUnitTextSize =
            typedArray.getDimension(R.styleable.BarChart_valueUnitTextSize, valueUnitTextSize)
        barUnreachedColor =
            typedArray.getColor(R.styleable.BarChart_barUnreachedColor, barUnreachedColor)
        barColor = typedArray.getColor(R.styleable.BarChart_barColor, barColor)
        barMaxWidth = typedArray.getDimension(R.styleable.BarChart_barMaxWidth, barMaxWidth)
        barMinWidth = typedArray.getDimension(R.styleable.BarChart_barMinWidth, barMinWidth)
        targetLineColor = typedArray.getColor(R.styleable.BarChart_targetLineColor, targetLineColor)
        touchLineColor = typedArray.getColor(R.styleable.BarChart_touchLineColor, touchLineColor)
        touchInfoBackgroundColor = typedArray.getColor(
            R.styleable.BarChart_touchInfoBackgroundColor,
            touchInfoBackgroundColor
        )
        touchInfoTextSize =
            typedArray.getDimension(R.styleable.BarChart_touchInfoTextSize, touchInfoTextSize)
        touchInfoTextColor =
            typedArray.getColor(R.styleable.BarChart_touchInfoTextColor, touchInfoTextColor)
        touchInfoInnerHorizontalPadding = typedArray.getDimension(
            R.styleable.BarChart_touchInfoInnerHorizontalPadding,
            touchInfoInnerHorizontalPadding
        )
        touchInfoInnerVerticalPadding = typedArray.getDimension(
            R.styleable.BarChart_touchInfoInnerVerticalPadding,
            touchInfoInnerVerticalPadding
        )
        typedArray.recycle()

        initPaints()
    }

    private fun initPaints() {
        textPaint = Paint().apply {
            textSize = axisTextSize
            color = axisTextColor
        }
        valueUnitTextPaint = Paint().apply {
            textSize = valueUnitTextSize
            color = axisTextColor
        }
        axisLinePaint = Paint().apply {
            strokeWidth = axisLineWidth
            color = axisLineColor
        }
        barPaint = Paint().apply {
            color = barColor
            style = Paint.Style.FILL
        }
        touchInfoTextPaint = Paint().apply {
            textSize = touchInfoTextSize
            color = touchInfoTextColor
            textAlign = Paint.Align.CENTER
        }
        touchLinePaint = Paint().apply {
            color = touchLineColor
            strokeWidth = 1.dp
        }
        targetLinePaint = Paint().apply {
            color = targetLineColor
            strokeWidth = 1.dp
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }
        touchInfoBackgroundPaint = Paint().apply {
            color = touchInfoBackgroundColor
            style = Paint.Style.FILL
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //  高度为横坐标轴文字高度 + chartBottomMargin + chartHeight + chartTopMargin
        val textBounds = bounds
        textPaint.getTextBounds("0", 0, 1, textBounds)
        val xAxisTextHeight = textBounds.height()
        val height = xAxisTextHeight + chartBottomMargin + chartHeight + chartTopMargin
        val heightValue = MeasureSpec.makeMeasureSpec(height.toInt(), MeasureSpec.EXACTLY)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)


        Log.d(TAG, "onMeasure: height = $height, width = $widthSize")
        setMeasuredDimension(widthMeasureSpec, heightValue)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        /*canvas.drawRect(
            0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat(),
            Paint().apply {
                color = Color.RED
                strokeWidth = 1.dp
                style = Paint.Style.STROKE
            }
        )*/

        // 画图表单位
        drawValueUnitText(canvas)
        drawYAxis(canvas)
        drawBar(canvas)
        drawXAxis(canvas)
        drawTouchEvent(canvas)
    }

    val bounds = Rect()
    private fun drawValueUnitText(canvas: Canvas) {
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.getTextBounds(valueUnitText, 0, valueUnitText.length, bounds)
        val textHeight = bounds.height()
        val x = paddingStart
        val y = chartTopMargin / 2 + textHeight / 2
        canvas.drawText(valueUnitText, x.toFloat(), y.toFloat(), valueUnitTextPaint)
    }


    /**
     * 画纵坐标轴和坐标轴文字
     */
    private fun drawYAxis(canvas: Canvas) {
        // 纵坐标
        textPaint.textAlign = Paint.Align.LEFT
        val yValues = listOf(0, maxValue / 4, maxValue / 2, maxValue / 4 * 3, maxValue).reversed()
        val yValueMax = yValues.max()
        textPaint.getTextBounds(yValueMax.toString(), 0, yValueMax.toString().length, bounds)
        val yAxisTextWidth = bounds.width()
        val yAxisTextHeight = bounds.height()
        val yAxisTextX = paddingStart.toFloat()

        val yUnit = chartHeight / (yValues.size - 1)
        yValues.forEachIndexed { index, value ->
            if (index == yValues.size - 1) return@forEachIndexed
            val tY = chartTopMargin + yUnit * index + yAxisTextHeight
            canvas.drawText(value.toString(), yAxisTextX, tY, textPaint)
        }

        val yAxisX = paddingStart + yAxisTextWidth + chartLeftMargin
        val yAxisStartY = chartTopMargin
        val yAxisEndY = chartTopMargin + chartHeight
        canvas.drawLine(
            yAxisX, yAxisStartY, yAxisX, yAxisEndY,
            axisLinePaint
        )

        chartStartX = yAxisX
        chartBottomY = yAxisEndY
    }

    private fun drawBar(canvas: Canvas) {
        val chartStartX = chartStartX + axisLineWidth / 2
        val chartWidth = measuredWidth - paddingEnd - chartStartX
        var barWidth = (chartWidth - (data.size - 1) * barMinSpace) / data.size
        barWidth = barWidth.coerceIn(barMinWidth, barMaxWidth)
        val space = (chartWidth - barWidth * data.size) / (data.size - 1)
        val radius = barWidth / 2
        barRanges.clear()
        data.map { it.value }.forEachIndexed { index, value ->
            val barHeight = (chartHeight * value / maxValue)
            barPaint.color = when {
                targetValue <= 0 -> barColor
                value < targetValue -> barUnreachedColor
                else -> barColor
            }
            val left = chartStartX + (space + barWidth) * index
            val top = (chartTopMargin + chartHeight - barHeight + radius).coerceAtMost(chartBottomY)
            val right = left + barWidth
            val bottom = chartTopMargin + chartHeight

            if (value != 0) {
                val rect = RectF(left, top, right, bottom)
                canvas.drawRect(rect, barPaint)
                // 画一个向上的半圆
                canvas.drawArc(
                    RectF(left, top - radius, left + barWidth, top + radius + 1),
                    180f, 180f, true, barPaint
                )
            }
            barRanges.add(BarRange(left = left, right = right, bottom = bottom, top = top - radius))
        }

        if (targetValue > 0) {
            val targetY = chartTopMargin + chartHeight * (maxValue - targetValue) / maxValue
            canvas.drawLine(
                chartStartX, targetY, measuredWidth - paddingEnd.toFloat(), targetY,
                targetLinePaint
            )
        }
    }


    private fun drawXAxis(canvas: Canvas) {
        // 横坐标
        val xAxisEndX = measuredWidth - paddingEnd.toFloat()
        canvas.drawLine(
            chartStartX - axisLineWidth / 2, chartBottomY, xAxisEndX, chartBottomY,
            axisLinePaint
        )

        data.map { it.label }.forEachIndexed { index, value ->
            val barRange = barRanges[index]
            val tX = when (index) {
                0 -> barRange.left
                data.size - 1 -> {
                    textPaint.getTextBounds(value, 0, value.length, bounds)
                    measuredWidth - paddingEnd
                }

                else -> barRange.left + (barRange.right - barRange.left) / 2
            }.toFloat()
            val tY = measuredHeight
            textPaint.textAlign = when (index) {
                0 -> Paint.Align.LEFT
                data.size - 1 -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            canvas.drawText(value, tX, tY.toFloat(), textPaint)
        }
    }

    private fun drawTouchEvent(canvas: Canvas) {
        if (touchedBarIndex == -1) return
        val barRange = barRanges[touchedBarIndex]
        val centerX = barRange.left + (barRange.right - barRange.left) / 2

        val text = "${data[touchedBarIndex].value}$valueUnit"
        touchInfoTextPaint.getTextBounds(text, 0, text.length, bounds)
        val textHeight = bounds.height()
        val textWidth = bounds.width()
        val infoWidth = textWidth + touchInfoInnerHorizontalPadding * 2
        val infoHeight = textHeight + touchInfoInnerVerticalPadding * 2
        val infoLeft = centerX - infoWidth / 2
        val infoTop = 0f
        val infoRight = infoLeft + infoWidth
        val infoBottom = infoTop + infoHeight
        val infoRect = RectF(infoLeft, infoTop, infoRight, infoBottom)
        canvas.drawRoundRect(
            infoRect, touchInfoCorner, touchInfoCorner,
            touchInfoBackgroundPaint
        )

        val textStartX = centerX
        val textStartY = infoTop + touchInfoInnerVerticalPadding + textHeight
        canvas.drawText(text, textStartX, textStartY, touchInfoTextPaint)
        canvas.drawLine(
            centerX, infoBottom, centerX, barRange.bottom,
            touchLinePaint
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val lastTouchBarIndex = touchedBarIndex
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                touchedBarIndex = barRanges.indexOfFirst { it.contains(x, y) }
                if (lastTouchBarIndex != touchedBarIndex) {
                    invalidate()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                touchedBarIndex = barRanges.indexOfFirst { it.contains(x, y) }
                if (lastTouchBarIndex != touchedBarIndex) {
                    invalidate()
                }
            }

            /*MotionEvent.ACTION_UP -> {
                touchedBarIndex = -1
                invalidate()
            }*/
        }
        Log.d(TAG, "onTouchEvent: touchedBarIndex = $touchedBarIndex")
        return true
    }


    fun setData(
        data: List<BarData>,
        maxValue: Int,
        targetValue: Int = 1000,
        valueUnit: String = "ml"
    ) {
        this.data = data
        this.maxValue = maxValue
        this.targetValue = targetValue
        this.valueUnit = valueUnit
        invalidate()
    }


    private data class BarRange(
        val left: Float,
        val right: Float,
        val top: Float,
        val bottom: Float
    ) {
        fun contains(x: Float, y: Float): Boolean {
            return x in left..right
        }
    }

    companion object {
        const val TAG = "BarChart"

        data class BarData(val label: String, val value: Int)
    }
}
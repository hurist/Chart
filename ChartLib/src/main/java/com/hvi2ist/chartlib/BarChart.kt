package com.hvi2ist.chartlib

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.hvi2ist.chartlib.util.ChartUtil
import com.hvi2ist.chartlib.util.dp
import com.hvi2ist.chartlib.util.sp
import com.hvi2ist.chartlib.util.toBitmap
import kotlin.math.abs

class BarChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetValue = -1f
    private var maxValue = 2000L
    private var data = listOf<BarData>()

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


    // 横纵轴文字和单位文字的颜色
    private var axisTextColor = Color.BLACK

    // 横纵轴文字的大小
    private var axisTextSize = 12.sp
    private var axisLineColor = Color.BLACK
    private var axisLineWidth = 4.dp

    // 未到达目标值的颜色
    private var barUnreachedColor = Color.GRAY
    private var barColor = Color.BLUE
    private var stackBarColor = Color.RED
    private var barMaxWidth = 18.dp
    private var barMinWidth = 2.dp
    private var targetLineColor = Color.RED

    // 柱状数据最小间隔
    private var barMinSpace = 2.dp

    private var touchLineColor = Color.RED
    private var infoPos = PosType.TOP
    private var infoPadding = 20.dp

    private lateinit var textPaint: Paint
    private lateinit var axisLinePaint: Paint
    private lateinit var barPaint: Paint
    private lateinit var touchLinePaint: Paint
    private lateinit var targetLinePaint: Paint

    private var infoLayoutId = -1
    private val childViewContainer = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    var childInfoView: View? = null
        set(value) {
            field = value
            if (value != null) {
                value.clipToOutline = false
                if (value !is ViewGroup) { // 得有一个容器，不然可能会出现childInfoView绘制出来只有背景，没有内容的情况
                    childViewContainer.removeAllViews()
                    childViewContainer.addView(value)
                    field = childViewContainer
                }
            }
            requestLayout()
            invalidate()
        }

    init {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.BarChart, defStyleAttr, 0)
        chartBottomMargin =
            typedArray.getDimension(R.styleable.BarChart_chartBottomMargin, chartBottomMargin)
        chartLeftMargin =
            typedArray.getDimension(R.styleable.BarChart_chartLeftMargin, chartLeftMargin)
        axisTextColor = typedArray.getColor(R.styleable.BarChart_axisTextColor, axisTextColor)
        axisTextSize = typedArray.getDimension(R.styleable.BarChart_axisTextSize, axisTextSize)
        axisLineColor = typedArray.getColor(R.styleable.BarChart_axisLineColor, axisLineColor)
        axisLineWidth = typedArray.getDimension(R.styleable.BarChart_axisLineWidth, axisLineWidth)
        barUnreachedColor =
            typedArray.getColor(R.styleable.BarChart_barUnreachedColor, barUnreachedColor)
        barColor = typedArray.getColor(R.styleable.BarChart_barColor, barColor)
        stackBarColor = typedArray.getColor(R.styleable.BarChart_stackBarColor, stackBarColor)
        barMaxWidth = typedArray.getDimension(R.styleable.BarChart_barMaxWidth, barMaxWidth)
        barMinWidth = typedArray.getDimension(R.styleable.BarChart_barMinWidth, barMinWidth)
        targetLineColor = typedArray.getColor(R.styleable.BarChart_targetLineColor, targetLineColor)
        touchLineColor = typedArray.getColor(R.styleable.BarChart_touchLineColor, touchLineColor)
        infoLayoutId = typedArray.getResourceId(R.styleable.BarChart_infoLayout, -1)
        infoPos = PosType.fromValue(typedArray.getInt(R.styleable.BarChart_infoPos, infoPos.value))
        infoPadding = typedArray.getDimension(R.styleable.BarChart_infoPadding, infoPadding)
        typedArray.recycle()

        if (infoLayoutId != -1) {
            childInfoView = LayoutInflater.from(context).inflate(infoLayoutId, null, false)
        }

        initPaints()
    }

    private fun initPaints() {
        textPaint = Paint().apply {
            textSize = axisTextSize
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
        touchLinePaint = Paint().apply {
            color = touchLineColor
            strokeWidth = 1.dp
        }
        targetLinePaint = Paint().apply {
            color = targetLineColor
            strokeWidth = 1.dp
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        //  高度为横坐标轴文字高度
        val textBounds = bounds
        textPaint.getTextBounds("0", 0, 1, textBounds)
        val xAxisTextHeight = textBounds.height()
        chartHeight = measuredHeight - chartBottomMargin - xAxisTextHeight

        // 测量子视图（如果存在）
        childInfoView?.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), // 宽度为 wrap_content
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)  // 高度为 wrap_content
        )
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 保存当前的 Canvas 状态
        val saveCount = canvas.save()

        // 清除裁剪限制
        canvas.clipRect(-width, -height, width * 2, height * 2)
        // 画图表单位
        drawYAxis(canvas)
        drawBar(canvas)
        drawXAxis(canvas)
        //drawTouchEvent(canvas)
        drawChildInfoView(canvas)
        canvas.restoreToCount(saveCount)
    }

    val bounds = Rect()

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
            val tY = yUnit * index + yAxisTextHeight
            canvas.drawText(value.toString(), yAxisTextX, tY, textPaint)
        }

        val yAxisX = paddingStart + yAxisTextWidth + chartLeftMargin
        val yAxisStartY = 0f
        val yAxisEndY = chartHeight
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
        data.forEachIndexed { index, data ->
            val value = data.value + data.value2

            val barHeight = (chartHeight * value / maxValue).coerceAtLeast(2.dp) // 最小高度为2dp, 不然看不见
            barPaint.color = when {
                targetValue <= 0 -> barColor
                value < targetValue -> barUnreachedColor
                else -> barColor
            }
            val left = chartStartX + (space + barWidth) * index
            val top = (chartHeight - barHeight).coerceAtMost(chartBottomY)
            val top2 = top + (chartHeight * data.value2 / maxValue).coerceAtMost(chartBottomY)
            val right = left + barWidth
            val bottom = chartHeight

            if (value != 0f) {
                if (value == data.value2) {
                    barPaint.color = stackBarColor
                } else {
                    barPaint.color = barColor
                }
                val rect = RectF(left, (top + radius).coerceAtMost(chartBottomY), right, bottom)
                canvas.drawRect(rect, barPaint)
                canvas.save()
                canvas.clipRect(left, top, right, bottom)
                // 画一个向上的半圆
                val outArcRect = RectF(left, top, left + barWidth, top + radius * 2 + 1)
                canvas.drawArc(
                    outArcRect,
                    0f, -180f, false, barPaint
                )
                canvas.restore()

                if (data.value2 != value && data.value2 > 0) {
                    barPaint.color = stackBarColor
                    val totalHeight = top2 - top
                    Log.d(TAG, "drawBar: totalHeight = $totalHeight")
                    if (totalHeight >= radius * 2) {
                        val rect1 = RectF(left, top, right, top2)
                        canvas.drawRoundRect(rect1, radius, radius, barPaint)
                    } else {
                        if (totalHeight > radius) {
                            val rect1 = RectF(left, top + radius, right, top2)
                            canvas.drawRect(rect1, barPaint)
                            canvas.drawArc(
                                RectF(left, top, left + barWidth, top + radius * 2 + 1),
                                0f, -180f, false, barPaint
                            )
                        } else {
                            canvas.save()
                            canvas.clipRect(left, top, left + barWidth, top2)
                            canvas.drawArc(
                                RectF(left, top, left + barWidth, top + radius * 2 + 1),
                                0f, -180f, false, barPaint
                            )
                            canvas.restore()
                        }
                    }
                }
            }
            barRanges.add(BarRange(left = left, right = right, bottom = bottom, top = top - radius))
        }

        if (targetValue > 0) {
            val targetY = chartHeight * (maxValue - targetValue) / maxValue
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

    private fun drawChildInfoView(canvas: Canvas) {
        // 绘制子视图（如果存在）
        childInfoView?.let { child ->
            if (touchedBarIndex == -1 || onTouchBarListener == null) return
            val bitmap = child.toBitmap()
            val barRange = barRanges[touchedBarIndex]
            val centerX = barRange.left + (barRange.right - barRange.left) / 2
            val lineTopY = if (infoPos == PosType.ABOVE) {
                barRange.top - infoPadding
            } else {
                -infoPadding
            }
            val childY = lineTopY - child.measuredHeight

            var translationX = (centerX - child.measuredWidth / 2).coerceAtLeast(0f)
            if (translationX + child.measuredWidth > measuredWidth) {
                translationX = measuredWidth - child.measuredWidth.toFloat()
            }

            canvas.drawBitmap(bitmap, translationX, childY, Paint())

            canvas.drawLine(
                centerX, lineTopY, centerX, barRange.bottom,
                touchLinePaint
            )
        }
    }

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val child = childInfoView ?: return false
        if (onTouchBarListener == null) return false
        val lastTouchBarIndex = touchedBarIndex
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                lastTouchX = x
                lastTouchY = y
                touchedBarIndex = barRanges.indexOfFirst { it.contains(x, y) }
                callbackTouchListener(lastTouchBarIndex, child)
                parent.requestDisallowInterceptTouchEvent(true)
                postInvalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                touchedBarIndex = barRanges.indexOfFirst { it.contains(x, y) }
                callbackTouchListener(lastTouchBarIndex, child)
                if (abs(y - lastTouchY) > 30) {
                    parent.requestDisallowInterceptTouchEvent(false)
                } else {
                    lastTouchX = x
                    lastTouchY = y
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                postInvalidate()
            }

            else -> {
                touchedBarIndex = -1
                //callbackTouchListener(lastTouchBarIndex, child)
                parent.requestDisallowInterceptTouchEvent(false)
                postInvalidate()
            }
        }
        Log.d(TAG, "onTouchEvent: touchedBarIndex = $touchedBarIndex")
        return true
    }

    private fun isVerticalScroll(event: MotionEvent): Boolean {
        // 根据滑动角度或者距离判断
        val dx = event.x - lastTouchX
        val dy = event.y - lastTouchY
        return Math.abs(dy) > Math.abs(dx)
    }

    private fun callbackTouchListener(lastTouchBarIndex: Int, child: View) {
        if (touchedBarIndex != -1 && touchedBarIndex != lastTouchBarIndex) {
            runCatching {
                onTouchBarListener?.invoke(child, data[touchedBarIndex], touchedBarIndex)
            }.onFailure {
                it.printStackTrace()
            }
        }
        if (touchedBarIndex != lastTouchBarIndex) {
            postInvalidate()
        }
    }


    fun setData(
        data: List<BarData>,
        targetValue: Float = -1f,
    ) {
        this.data = data
        this.maxValue = ChartUtil.getChartMaxValue(data.maxOf { it.value + it.value2 }, targetValue)
        this.targetValue = targetValue
        invalidate()
    }

    private var onTouchBarListener: ((infoView: View, barData: BarData, index: Int) -> Unit)? = null
    fun setOnTouchBarListener(callback: (infoView: View, barData: BarData, index: Int) -> Unit) {
        onTouchBarListener = callback
    }


    private data class BarRange(
        val left: Float,
        val right: Float,
        val top: Float,
        val bottom: Float,
        val top2: Float = 0f
    ) {
        fun contains(x: Float, y: Float): Boolean {
            return x in left..right
        }
    }

    companion object {
        const val TAG = "BarChart"

        data class BarData(val label: String, val value: Float, val value2: Float = 0f)
    }
}
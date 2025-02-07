package com.hvi2ist.chartlib

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.hvi2ist.chartlib.util.dp
import com.hvi2ist.chartlib.util.sp
import com.hvi2ist.chartlib.util.toBitmap
import kotlin.math.abs

class LineChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var maxValue = 2000
    private var data = listOf<LineData>()
    private var rangeColors = listOf<RangeColor>()

    private var chartStartX = 0f
    private var chartBottomY = 0f
    private var touchedBarIndex = -1


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
    /*private var barUnreachedColor = Color.GRAY
    private var barColor = Color.BLUE
    private var barMaxWidth = 18.dp
    private var barMinWidth = 2.dp

    // 柱状数据最小间隔
    private var barMinSpace = 2.dp*/
    private var targetLineColor = Color.RED
    private val dotPos = mutableListOf<Point>()


    private var lineColor = Color.GREEN
    private var lineWidth = 1.dp
    private var dotRadius = 4.dp
    private var dotSelectedRadius = 5.dp
    private val chartHorizontalPadding = 6.dp

    private var touchLineColor = Color.RED

    private lateinit var textPaint: Paint
    private lateinit var axisLinePaint: Paint
    private lateinit var touchLinePaint: Paint
    private lateinit var targetLinePaint: Paint
    private lateinit var linePaint: Paint
    private lateinit var dotPaint: Paint


    private var infoPos = PosType.TOP
    private var infoPadding = 20.dp
    private var infoLayoutId = -1
    private val childViewContainer = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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
            context.obtainStyledAttributes(attrs, R.styleable.LineChart, defStyleAttr, 0)
        chartBottomMargin =
            typedArray.getDimension(R.styleable.LineChart_chartBottomMargin, chartBottomMargin)
        chartLeftMargin =
            typedArray.getDimension(R.styleable.LineChart_chartLeftMargin, chartLeftMargin)
        axisTextColor = typedArray.getColor(R.styleable.LineChart_axisTextColor, axisTextColor)
        axisTextSize = typedArray.getDimension(R.styleable.LineChart_axisTextSize, axisTextSize)
        axisLineColor = typedArray.getColor(R.styleable.LineChart_axisLineColor, axisLineColor)
        axisLineWidth = typedArray.getDimension(R.styleable.LineChart_axisLineWidth, axisLineWidth)
        lineWidth = typedArray.getDimension(R.styleable.LineChart_lineWidth, lineWidth)
        lineColor = typedArray.getColor(R.styleable.LineChart_lineColor, lineColor)
        dotRadius = typedArray.getDimension(R.styleable.LineChart_dotRadius, dotRadius)
        dotSelectedRadius = typedArray.getDimension(R.styleable.LineChart_dotSelectedRadius, dotSelectedRadius)
        touchLineColor = typedArray.getColor(R.styleable.LineChart_touchLineColor, touchLineColor)
        infoLayoutId = typedArray.getResourceId(R.styleable.LineChart_infoLayout, -1)
        infoPos = PosType.fromValue(typedArray.getInt(R.styleable.LineChart_infoPos, infoPos.value))
        infoPadding = typedArray.getDimension(R.styleable.LineChart_infoPadding, infoPadding)
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
        touchLinePaint = Paint().apply {
            color = touchLineColor
            strokeWidth = 1.dp
        }
        targetLinePaint = Paint().apply {
            color = targetLineColor
            strokeWidth = 1.dp
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }
        linePaint = Paint().apply {
            color = lineColor
            strokeWidth = lineWidth
        }
        dotPaint = Paint().apply {
            color = lineColor
            style = Paint.Style.FILL
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
        canvas.drawColor(Color.TRANSPARENT)
        // 保存当前的 Canvas 状态
        val saveCount = canvas.save()

        // 清除裁剪限制
        canvas.clipRect(-width, -height, width * 2, height * 2)
        /*canvas.drawRect(
            0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat(),
            Paint().apply {
                color = Color.RED
                strokeWidth = 1.dp
                style = Paint.Style.STROKE
            }
        )*/

        // 画图表单位
        drawYAxis(canvas)
        calcDotPos()
        drawXAxis(canvas)
        drawLine(canvas)
        drawDot(canvas)
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

    private fun calcDotPos() {
        dotPos.clear()
        val chartStartX = chartStartX + axisLineWidth / 2 + chartHorizontalPadding
        val chartWidth = measuredWidth - paddingEnd - chartStartX - 2 * chartHorizontalPadding
        val xUnit = (chartWidth) / (data.size - 1)
        val yUnit = chartHeight / maxValue
        data.map {
            it.value
        }.forEachIndexed { index, value ->
            val x = chartStartX + xUnit * index
            val y = if (value == NO_VALUE) NO_VALUE_Y else chartBottomY - value * yUnit
            dotPos.add(Point(x.toInt(), y.toInt()))
        }
    }

    private fun drawDot(canvas: Canvas) {
        dotPos.mapIndexed { index, point ->
            if (point.y == NO_VALUE_Y.toInt()) return@mapIndexed
            val dotColor = when {
                rangeColors.isEmpty() -> lineColor
                else -> {
                    val value = data[index].value
                    rangeColors.firstOrNull {
                        value in it.range.first..it.range.second
                    }?.color ?: lineColor
                }
            }
            dotPaint.color = dotColor
            if (index != touchedBarIndex) {
                canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), dotRadius, dotPaint)
            } else {
                dotPaint.color = dotColor
                canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), dotSelectedRadius, dotPaint)
                dotPaint.color = Color.WHITE
                canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), dotSelectedRadius - 1.dp, dotPaint)
            }
        }
    }

    private fun drawLine(canvas: Canvas) {

        val validDotPost = dotPos.filter { it.y != NO_VALUE_Y.toInt() }
        if (validDotPost.size < 2) return
        val first = dotPos.indexOfFirst { it.y != NO_VALUE_Y.toInt() }
        val last = dotPos.indexOfLast { it.y != NO_VALUE_Y.toInt() }

        dotPos.mapIndexed { index, point ->
            if (index == 0) return@mapIndexed
            if (point.y == NO_VALUE_Y.toInt()) return@mapIndexed
            if (index < first || index > last) return@mapIndexed
            val lastValidIndex = dotPos.subList(0, index).indexOfLast { it.y != NO_VALUE_Y.toInt() }
            if (lastValidIndex == -1) return@mapIndexed
            val lastValidData = data[lastValidIndex]
            val lasValidPos = dotPos[lastValidIndex]
            val color = when {
                rangeColors.isEmpty() -> lineColor
                else -> {
                    val value = lastValidData.value
                    rangeColors.firstOrNull {
                        value in it.range.first..it.range.second
                    }?.color ?: lineColor
                }
            }
            linePaint.color = color
            canvas.drawLine(
                lasValidPos.x.toFloat(), lasValidPos.y.toFloat(), point.x.toFloat(), point.y.toFloat(), linePaint
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
            val pos = dotPos[index]
            val tX = pos.x.toFloat()
            val tY = measuredHeight
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(value, tX, tY.toFloat(), textPaint)
        }
    }

    private fun drawChildInfoView(canvas: Canvas) {
        // 绘制子视图（如果存在）
        childInfoView?.let { child ->
            if (touchedBarIndex == -1 || onTouchBarListener == null) return
            val bitmap = child.toBitmap()
            val pos = dotPos[touchedBarIndex]
            val centerX = pos.x.toFloat()
            val lineTopY = if (infoPos == PosType.ABOVE) {
                pos.y - infoPadding
            } else {
                -infoPadding
            }
            val childY = lineTopY - child.measuredHeight

            var translationX = (centerX - child.measuredWidth / 2).coerceAtLeast(0f)
            if (translationX + child.measuredWidth > measuredWidth) {
                translationX = measuredWidth - child.measuredWidth.toFloat()
            }

            canvas.drawBitmap(bitmap, translationX, childY.toFloat(), Paint())

            canvas.drawLine(
                centerX, lineTopY.toFloat(), centerX, chartBottomY,
                touchLinePaint
            )
        }
    }

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val childView = childInfoView ?: return false
        if (onTouchBarListener == null) return false
        val lastTouchBarIndex = touchedBarIndex
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                callbackTouchListener(x, lastTouchBarIndex, childView)
                parent.requestDisallowInterceptTouchEvent(true)
                postInvalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                callbackTouchListener(x, lastTouchBarIndex, childView)
                if (abs(y - lastTouchY) > 50) {
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

    private fun callbackTouchListener(
        x: Float,
        lastTouchBarIndex: Int,
        childView: View
    ) {
        touchedBarIndex = dotPos.indexOfFirst {
            x <= it.x + dotRadius && x >= it.x - dotRadius
        }
        if (dotPos.getOrNull(touchedBarIndex)?.y?.toFloat() == NO_VALUE_Y) {
            touchedBarIndex = -1
        }
        if (touchedBarIndex != -1 && lastTouchBarIndex != touchedBarIndex) {
            onTouchBarListener?.invoke(childView, data[touchedBarIndex], touchedBarIndex)
        }
        if (lastTouchBarIndex != touchedBarIndex) {
            postInvalidate()
        }
    }


    fun setData(
        data: List<LineData>,
        maxValue: Int,
        rangeColors: List<RangeColor>
    ) {
        this.data = data
        this.maxValue = maxValue
        this.rangeColors = rangeColors
        invalidate()
    }

    private var onTouchBarListener: ((infoView: View, barData: LineData, index: Int) -> Unit)? = null
    fun setOnTouchBarListener(callback: (infoView: View, barData: LineData, index: Int) -> Unit) {
        onTouchBarListener = callback
    }


    companion object {
        const val TAG = "LineChart"
        const val NO_VALUE = -1f
        private const val NO_VALUE_Y = -10086f
    }



    class RangeColor(
        val range: Pair<Float, Float>,
        val color: Int
    )

    class LineData(
        val label: String,
        val value: Float
    )
}
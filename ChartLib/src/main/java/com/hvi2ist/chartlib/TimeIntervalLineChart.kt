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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.hvi2ist.chartlib.util.disableParentClip
import com.hvi2ist.chartlib.util.dp
import com.hvi2ist.chartlib.util.isMorning
import com.hvi2ist.chartlib.util.minutesBetween
import com.hvi2ist.chartlib.util.sp
import com.hvi2ist.chartlib.util.toBitmap
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlin.math.abs

class TimeIntervalLineChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data = listOf<TimeLineData>()
    private val maxValue = 1440

    private var chartStartX = 0f
    private var chartBottomY = 0f
    private var touchedDotIndex = -1


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
    private val dotPos = mutableListOf<DotPos>()

    private var startDotColor = Color.RED
    private var endDotColor = Color.BLUE

    private var dotRadius = 4.dp
    private var dotSelectedRadius = 5.dp
    private val chartHorizontalPadding = 6.dp

    private var touchLineColor = Color.RED

    private lateinit var textPaint: Paint
    private lateinit var axisLinePaint: Paint
    private lateinit var touchLinePaint: Paint
    private lateinit var targetLinePaint: Paint
    private lateinit var dotPaint: Paint


    private var infoPos = PosType.TOP
    private var infoPadding = 20.dp
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
            context.obtainStyledAttributes(
                attrs,
                R.styleable.TimeIntervalLineChart,
                defStyleAttr,
                0
            )
        chartBottomMargin =
            typedArray.getDimension(
                R.styleable.TimeIntervalLineChart_chartBottomMargin,
                chartBottomMargin
            )
        chartLeftMargin =
            typedArray.getDimension(
                R.styleable.TimeIntervalLineChart_chartLeftMargin,
                chartLeftMargin
            )
        axisTextColor =
            typedArray.getColor(R.styleable.TimeIntervalLineChart_axisTextColor, axisTextColor)
        axisTextSize =
            typedArray.getDimension(R.styleable.TimeIntervalLineChart_axisTextSize, axisTextSize)
        axisLineColor =
            typedArray.getColor(R.styleable.TimeIntervalLineChart_axisLineColor, axisLineColor)
        axisLineWidth =
            typedArray.getDimension(R.styleable.TimeIntervalLineChart_axisLineWidth, axisLineWidth)
        startDotColor =
            typedArray.getColor(R.styleable.TimeIntervalLineChart_startDotColor, startDotColor)
        endDotColor =
            typedArray.getColor(R.styleable.TimeIntervalLineChart_endDotColor, endDotColor)
        dotRadius = typedArray.getDimension(R.styleable.TimeIntervalLineChart_dotRadius, dotRadius)
        dotSelectedRadius = typedArray.getDimension(
            R.styleable.TimeIntervalLineChart_dotSelectedRadius,
            dotSelectedRadius
        )
        touchLineColor =
            typedArray.getColor(R.styleable.TimeIntervalLineChart_touchLineColor, touchLineColor)
        infoLayoutId = typedArray.getResourceId(R.styleable.TimeIntervalLineChart_infoLayout, -1)
        infoPos = PosType.fromValue(typedArray.getInt(R.styleable.TimeIntervalLineChart_infoPos, infoPos.value))
        infoPadding = typedArray.getDimension(R.styleable.TimeIntervalLineChart_infoPadding, infoPadding)
        typedArray.recycle()

        if (infoLayoutId != -1) {
            childInfoView = LayoutInflater.from(context).inflate(infoLayoutId, null, false)
        }

        initPaints()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        disableParentClip()
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
        dotPaint = Paint().apply {
            color = startDotColor
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
        val maxValue = 1440 // 一天的分钟数
        val yValues = listOf(0, maxValue / 4, maxValue / 2, maxValue / 4 * 3, maxValue).reversed()
        val yValueMax = yValues.max()
        textPaint.getTextBounds(yValueMax.toString(), 0, yValueMax.toString().length, bounds)
        val yAxisTextWidth = bounds.width()
        val yAxisTextHeight = bounds.height()
        val yAxisTextX = paddingStart.toFloat()

        val space = (chartHeight - 5 * yAxisTextHeight) / (yValues.size - 1)
        yValues.forEachIndexed { index, value ->
            val tY = (index + 1) * yAxisTextHeight + index * space

            val text = when (index) {
                0 -> "12:00"
                1 -> "06:00"
                2 -> "00:00"
                3 -> "18:00"
                else -> "12:01"
            }
            canvas.drawText(text, yAxisTextX, tY.coerceAtMost(chartHeight), textPaint)
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

    val time0Clock = LocalTime(0, 0)
    val time1201Clock = LocalTime(11, 59)
    val startTime = LocalTime(0, 0)
    val endTime = LocalTime(0, 0)
    private fun calcDotPos() {
        dotPos.clear()
        val chartStartX = chartStartX + axisLineWidth / 2 + chartHorizontalPadding
        val chartWidth = measuredWidth - paddingEnd - chartStartX - 2 * chartHorizontalPadding
        val xUnit = (chartWidth) / (data.size - 1)
        val yUnit = chartHeight / maxValue

        val chartCenterY = chartBottomY / 2
        val belowDate = data.map { it.belongDate }
        data.forEachIndexed { index, value ->
            val startTime = value.startTime
            val endTime = value.endTime
            // 小于等于12点，位于图表上半部分
            if (startTime.isMorning()) {
                val xIndex = belowDate.indexOfFirst { it == value.belongDate } // 确定这个时间在横轴的哪一天
                if (xIndex != -1) {
                    val minutes = startTime.minutesBetween(time0Clock)
                    val y = chartCenterY - minutes * yUnit
                    val x = chartStartX + xIndex * xUnit
                    dotPos.add(DotPos(value.belongDateString,true, x, y))
                }
            } else {
                val xIndex = belowDate.indexOfFirst { it == value.belongDate } // 确定这个时间在横轴的哪一天
                if (xIndex != -1) {
                    val minutes = startTime.minutesBetween(time1201Clock)
                    val y = chartBottomY - minutes * yUnit
                    val x = chartStartX + xIndex * xUnit
                    dotPos.add(DotPos(value.belongDateString,true, x, y))
                }
            }


            if (endTime.isMorning()) {
                val xIndex = belowDate.indexOfFirst { it == value.belongDate } // 确定这个时间在横轴的哪一天
                if (xIndex != -1) {
                    val minutes = endTime.minutesBetween(time0Clock)
                    val y = chartCenterY - minutes * yUnit
                    val x = chartStartX + xIndex * xUnit
                    dotPos.add(DotPos(value.belongDateString,false, x, y))
                }
            } else {
                val xIndex = belowDate.indexOfFirst { it == value.belongDate } // 确定这个时间在横轴的哪一天
                if (xIndex != -1) {
                    val minutes = endTime.minutesBetween(time1201Clock)
                    val y = chartBottomY - minutes * yUnit
                    val x = chartStartX + xIndex * xUnit
                    dotPos.add(DotPos(value.belongDateString,false, x, y))
                }
            }
        }
    }

    private fun drawDot(canvas: Canvas) {
        dotPos.mapIndexed { index, point ->
            val dotColor = if(point.isStart) startDotColor else endDotColor
            dotPaint.color = dotColor
            if (index != touchedDotIndex) {
                canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), dotRadius, dotPaint)
            } else {
                canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), dotSelectedRadius, dotPaint)
                dotPaint.color = Color.WHITE
                canvas.drawCircle(
                    point.x.toFloat(),
                    point.y.toFloat(),
                    dotSelectedRadius - 1.dp,
                    dotPaint
                )
            }
        }
    }


    private fun drawXAxis(canvas: Canvas) {
        // 横坐标
        val xAxisEndX = measuredWidth - paddingEnd.toFloat()
        canvas.drawLine(
            chartStartX - axisLineWidth / 2, chartBottomY, xAxisEndX, chartBottomY,
            axisLinePaint
        )

        val width = xAxisEndX - chartStartX - 2 * chartHorizontalPadding
        val startX = chartStartX + chartHorizontalPadding
        val endX = xAxisEndX - chartHorizontalPadding
        data.map { it.label }.forEachIndexed { index, text ->
            val textX = when (index) {
                0 -> startX
                data.size - 1 -> endX
                else -> startX + width / (data.size - 1) * index
            }.toFloat()
            textPaint.textAlign = when (index) {
                0 -> Paint.Align.LEFT
                data.size - 1 -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            canvas.drawText(text, textX, measuredHeight.toFloat(), textPaint)
        }
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawChildInfoView(canvas: Canvas) {
        // 绘制子视图（如果存在）
        childInfoView?.let { child ->
            if (touchedDotIndex == -1 || onTouchBarListener == null) return
            val bitmap = child.toBitmap()
            val pos = dotPos[touchedDotIndex]
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
        val lastTouchBarIndex = touchedDotIndex
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                touchedDotIndex = dotPos.indexOfFirst { it.isTouching(x, y) }
                callbackTouchListener(lastTouchBarIndex, childView)
                parent.requestDisallowInterceptTouchEvent(true)
                postInvalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                touchedDotIndex = dotPos.indexOfFirst { it.isTouching(x, y) }
                callbackTouchListener(lastTouchBarIndex, childView)
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
                touchedDotIndex = -1
                //callbackTouchListener(lastTouchBarIndex, child)
                parent.requestDisallowInterceptTouchEvent(false)
                postInvalidate()
            }
        }
        Log.d(TAG, "onTouchEvent: touchedBarIndex = $touchedDotIndex")
        return true
    }

    private fun callbackTouchListener(lastTouchBarIndex: Int, childView: View) {
        if (touchedDotIndex != -1 && lastTouchBarIndex != touchedDotIndex) {
            val index = data.indexOfFirst {
                it.belongDateString == dotPos[touchedDotIndex].belongDateString
            }
            onTouchBarListener?.invoke(childView, data[index], touchedDotIndex)
        }
        if (lastTouchBarIndex != touchedDotIndex) {
            postInvalidate()
        }
    }


    fun setData(
        data: List<TimeLineData>,
    ) {
        this.data = data
        invalidate()
    }

    private var onTouchBarListener: ((infoView: View, barData: TimeLineData, index: Int) -> Unit)? =
        null

    fun setOnTouchBarListener(callback: (infoView: View, barData: TimeLineData, index: Int) -> Unit) {
        onTouchBarListener = callback
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
        const val NO_VALUE = -1f
        private const val NO_VALUE_Y = -10086f
        private val touchRange = 10.dp

    }

    private class DotPos(
        val belongDateString: String,
        val isStart: Boolean,
        val x: Float,
        val y: Float
    ) {
        private val touchRect = RectF().apply {
            left = (x - touchRange)
            right = (x + touchRange)
            top = (y - touchRange)
            bottom = (y + touchRange)
        }
        fun isTouching(x: Float, y: Float): Boolean {
            return touchRect.contains(x, y)
        }
    }

}

class TimeLineData(
    val belongDateString: String,
    val label: String,
    val startValue: String,
    val endValue: String
) {
    companion object {
        @OptIn(FormatStringsInDatetimeFormats::class)
        private val dateFormat = DateTimeComponents.Format {
            byUnicodePattern("uuuu-MM-dd HH:mm:ss")
        }
    }
    val belongDate = LocalDate.parse(belongDateString)
    val startDateTime = dateFormat.parse(startValue).toLocalDateTime()
    val startTime = startDateTime.time
    val endDateTime = dateFormat.parse(endValue).toLocalDateTime()
    val endTime = endDateTime.time

    override fun toString(): String {
        return """
            belongDate: $belongDateString
            label: $label
            startDateTime: $startValue
            endDateTime: $endValue
            """
    }
}
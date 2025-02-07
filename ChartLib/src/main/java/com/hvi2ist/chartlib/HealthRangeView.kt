package com.hvi2ist.chartlib

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.hvi2ist.chartlib.util.dp

class HealthRangeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var data: Data? = null
    private val rangeText: List<String>
        get() {
            return if (data == null) {
                emptyList()
            } else {
                listOf(
                    data!!.thinRange.first.toString(),
                    data!!.normalRange.first.toString(),
                    data!!.exceedRange.first.toString(),
                    data!!.obesityRange.first.toString(),
                    data!!.obesityRange.last.toString()
                )
            }
        }

    private var barHeight = 8.dp
    private var dotRadius = 8.dp
    /** 数字 */
    private var rangeTextSize = 13.dp
    /** 下方的标签 */
    private var rangeLabelTextSize = 11.dp
    private var textPadding = 6.dp
    private var textColor = 0xFF999999.toInt()

    private var textPaint: Paint = Paint()
    private var barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var label1 = "偏瘦"
    private var label2 = "正常"
    private var label3 = "超重"
    private var label4 = "肥胖"

    private var dotPos = DotPos.CENTER

    private val colors = intArrayOf(
        0xFF66B0FA.toInt(),
        0xFF67D075.toInt(),
        0xFFF5C035.toInt(),
        0xFFEF5C2E.toInt()
    )

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.HealthRangeView, defStyleAttr, 0)
        barHeight = a.getDimension(R.styleable.HealthRangeView_barHeight, barHeight)
        dotRadius = a.getDimension(R.styleable.HealthRangeView_dotRadius, dotRadius)
        rangeTextSize = a.getDimension(R.styleable.HealthRangeView_rangeTextSize, rangeTextSize)
        rangeLabelTextSize = a.getDimension(R.styleable.HealthRangeView_rangeLabelTextSize, rangeLabelTextSize)
        textPadding = a.getDimension(R.styleable.HealthRangeView_textPadding, textPadding)
        textColor = a.getColor(R.styleable.HealthRangeView_android_textColor, textColor)
        label1 = a.getString(R.styleable.HealthRangeView_label1Text) ?: label1
        label2 = a.getString(R.styleable.HealthRangeView_label2Text) ?: label2
        label3 = a.getString(R.styleable.HealthRangeView_label3Text) ?: label3
        label4 = a.getString(R.styleable.HealthRangeView_label4Text) ?: label4
        a.recycle()
    }

    private val bounds = Rect()
    private var rangeTextHeight = 0
    private var rangeLabelTextHeight = 0
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        textPaint.textSize = rangeTextSize
        textPaint.getTextBounds("0", 0, 1, bounds)
        rangeTextHeight = bounds.height()
        textPaint.textSize = rangeLabelTextSize
        textPaint.getTextBounds("0", 0, 1, bounds)
        rangeLabelTextHeight = bounds.height()
        val barHeight = maxOf(barHeight, dotRadius * 2)
        val height = (rangeTextHeight + textPadding + rangeLabelTextHeight + textPadding + barHeight).toInt()
        setMeasuredDimension(widthMeasureSpec, height)
    }

    private val path = Path()
    private val rect = RectF()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制上方的数字
        textPaint.textSize = rangeTextSize
        textPaint.color = textColor
        if (rangeText.isNotEmpty()) {
            rangeText.mapIndexed { index, text ->
                val textX = when (index) {
                    0 -> 0
                    rangeText.size - 1 -> width.toFloat()
                    else -> width / (rangeText.size - 1) * index.toFloat()
                }.toFloat()
                textPaint.textAlign = when (index) {
                    0 -> Paint.Align.LEFT
                    rangeText.size - 1 -> Paint.Align.RIGHT
                    else -> Paint.Align.CENTER
                }
                canvas.drawText(text, textX, rangeTextHeight.toFloat(), textPaint)
            }
        }

        // 绘制进度条
        // 分成4段，首尾带圆角，中间不带，每段间隔3dp
        val barCenterY = if(dotRadius * 2 >= barHeight) {
            rangeTextHeight + textPadding + dotRadius
        } else {
            rangeTextHeight + textPadding + barHeight / 2
        }
        val partWidth = (width - 3 * 3.dp) / 4
        val leftPosList = drawBar(barCenterY, partWidth, canvas)
        data?.let {
            val currentValue = it.currentValue.toFloat()
            var dotX: Float
            var dotColor: Int
            when {
                currentValue <= it.thinRange.last -> {
                    dotColor = colors[0]
                    val start = leftPosList[0]
                    dotX = start + partWidth * ((currentValue - it.thinRange.first) / (it.thinRange.last- it.thinRange.first))
                    dotX = adjustDotPos(start, dotX, partWidth)
                }
                currentValue <= it.normalRange.last -> {
                    dotColor = colors[1]
                    val start = leftPosList[1]
                    dotX = start + partWidth * ((currentValue - it.normalRange.first) / (it.normalRange.last - it.normalRange.first))
                    dotX = adjustDotPos(start, dotX, partWidth)
                }
                currentValue <= it.exceedRange.last -> {
                    dotColor = colors[2]
                    val start = leftPosList[2]
                    dotX = start + partWidth * ((currentValue - it.exceedRange.first) / (it.exceedRange.last - it.exceedRange.first))
                    dotX = adjustDotPos(start, dotX, partWidth)
                }
                else -> {
                    dotColor = colors[3]
                    val start = leftPosList[3]
                    dotX = start + partWidth * ((currentValue - it.obesityRange.first) / (it.obesityRange.last - it.obesityRange.first))
                    dotX = adjustDotPos(start, dotX, partWidth)
                }
            }
            barPaint.color = Color.WHITE
            canvas.drawCircle(dotX, barCenterY, dotRadius, barPaint)
            barPaint.color = dotColor
            canvas.drawCircle(dotX, barCenterY, dotRadius - 2.dp, barPaint)
        }
    }

    private fun adjustDotPos(startX: Float, dotX: Float, partWidth: Float): Float {
        return if (dotPos == DotPos.CENTER) {
            dotX.coerceIn(startX + dotRadius, startX + partWidth - dotRadius)
        } else {
            return dotX.coerceIn(startX, startX + partWidth)
        }
    }

    private fun drawBar(
        barCenterY: Float,
        partWidth: Float,
        canvas: Canvas
    ): List<Float> {
        val lefts = mutableListOf<Float>()
        val barTopY = barCenterY - barHeight / 2
        val barRadius = barHeight / 2

        textPaint.textSize = rangeLabelTextSize
        textPaint.color = textColor
        textPaint.textAlign = Paint.Align.CENTER

        repeat(4) { index ->
            val left = index * (partWidth + 3.dp)
            lefts.add(left)
            val right = left + partWidth
            val bottom = barTopY + barHeight
            barPaint.color = colors[index]
            when (index) {
                0 -> {
                    // 绘制第一段，只有左边有圆角
                    path.reset()
                    rect.set(left, barTopY, right, bottom)
                    path.addRoundRect(
                        rect,
                        floatArrayOf(barRadius, barRadius, 0f, 0f, 0f, 0f, barRadius, barRadius),
                        Path.Direction.CW
                    )
                    canvas.drawPath(path, barPaint)
                }

                3 -> {
                    // 绘制最后一段，只有右边有圆角
                    path.reset()
                    rect.set(left, barTopY, right, bottom)
                    path.addRoundRect(
                        rect,
                        floatArrayOf(0f, 0f, barRadius, barRadius, barRadius, barRadius, 0f, 0f),
                        Path.Direction.CW
                    )
                    canvas.drawPath(path, barPaint)
                }

                else -> {
                    canvas.drawRect(left, barTopY, right, bottom, barPaint)
                }
            }

            val text = when (index) {
                0 -> label1
                1 -> label2
                2 -> label3
                else -> label4
            }
            val textX = left + partWidth / 2
            canvas.drawText(text, textX, measuredHeight.toFloat(), textPaint)
        }
        return lefts
    }


    fun setData(data: Data) {
        this.data = data
        invalidate()
    }

    class Data(
        val thinRange: IntRange,
        val normalRange: IntRange,
        val exceedRange: IntRange,
        val obesityRange: IntRange,
        val currentValue: Int
    )


    enum class DotPos {
        // 贴边，即圆心可以到达边缘
        EDGE,
        // 在中间。即只能圆的边到达边缘
        CENTER
    }
}
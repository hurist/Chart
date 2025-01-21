package com.hvi2ist.chartlib

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.hvi2ist.chartlib.util.dp

class HealthRangeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    /**
     *      <attr name="barHeight"  format="dimension"/>
     *         <attr name="dotRadius"/>
     *         <attr name="rangeTextSize" format="dimension" />
     *         <attr name="rangeLabelTextSize" format="dimension" />
     *         <attr name="textPadding" format="dimension" />
     *         <attr name="android:textColor" />
     */
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
                    data!!.obesityRange.first.toString()
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
    }

    class Data(
        val thinRange: IntRange,
        val normalRange: IntRange,
        val exceedRange: IntRange,
        val obesityRange: IntRange,
        val currentValue: Int
    )
}
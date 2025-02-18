package com.hvi2ist.chartlib

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.hvi2ist.chartlib.util.dp
import com.hvi2ist.chartlib.util.toRectF

class StepProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progressBarHeight = 10.dp
    private var inactiveColor = Color.GRAY
    private var activeColor = Color.BLUE
    private var max = 100
    private var progress = 0
    private var iconSize = 20.dp
    private var iconMargin = 10.dp
    private var iconDrawable = context.getDrawable(android.R.drawable.ic_menu_camera)

    private val rect = Rect()

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.StepProgress)
        progressBarHeight = a.getDimension(R.styleable.StepProgress_spProgressBarHeight, progressBarHeight)
        inactiveColor = a.getColor(R.styleable.StepProgress_spInactiveColor, inactiveColor)
        activeColor = a.getColor(R.styleable.StepProgress_spActiveColor, activeColor)
        max = a.getInt(R.styleable.StepProgress_spMax, max)
        progress = a.getInt(R.styleable.StepProgress_spProgress, progress)
        iconSize = a.getDimension(R.styleable.StepProgress_spIconSize, iconSize)
        iconMargin = a.getDimension(R.styleable.StepProgress_spIconMargin, iconMargin)
        iconDrawable = a.getDrawable(R.styleable.StepProgress_spIconDrawable)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = progressBarHeight + iconSize + iconMargin
        setMeasuredDimension(widthMeasureSpec, height.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 进度条
        rect.set(
            0,
            (iconSize + iconMargin).toInt(),
            measuredWidth,
            measuredHeight
        )

        paint.color = inactiveColor
        canvas.drawRoundRect(
            rect.toRectF(),
            progressBarHeight / 2f,
            progressBarHeight / 2f,
            paint
        )

        val width = measuredWidth * progress / max
        rect.set(
            0,
            (iconSize + iconMargin).toInt(),
            width,
            measuredHeight
        )
        canvas.drawRoundRect(
            rect.toRectF(),
            progressBarHeight / 2f,
            progressBarHeight / 2f,
            paint.apply {
                color = activeColor
            }
        )

        // 图标
        val left = (width - iconSize).coerceAtLeast(0f)
        val top = 0
        val right = (left + iconSize).coerceAtMost(measuredWidth.toFloat())
        val bottom = iconSize
        rect.set(left.toInt(), top, right.toInt(), bottom.toInt())
        iconDrawable?.bounds = rect
        iconDrawable?.draw(canvas)
    }

    fun setProgress(progress: Int) {
        this.progress = progress
        invalidate()
    }

    fun setMax(max: Int) {
        this.max = max
        invalidate()
    }
}
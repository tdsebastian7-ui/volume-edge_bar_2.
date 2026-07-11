package com.example.volumegestureapp

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar

class VerticalSeekBar : SeekBar {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(h, w, oldh, oldw)
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(c: Canvas) {
        c.rotate(-90f)
        c.translate(-height.toFloat(), 0f)
        super.onDraw(c)
    }

    private var onChangeListener: OnSeekBarChangeListener? = null

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        super.setOnSeekBarChangeListener(l)
        this.onChangeListener = l
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isSelected = true
                isPressed = true
                onChangeListener?.onStartTrackingTouch(this)
                trackTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                trackTouchEvent(event)
            }
            MotionEvent.ACTION_UP -> {
                trackTouchEvent(event)
                onChangeListener?.onStopTrackingTouch(this)
                isSelected = false
                isPressed = false
            }
            MotionEvent.ACTION_CANCEL -> {
                onChangeListener?.onStopTrackingTouch(this)
                isSelected = false
                isPressed = false
            }
        }
        return true
    }

    private fun trackTouchEvent(event: MotionEvent) {
        val y = event.y
        val height = height
        var progressValue = max - (max * y / height).toInt()
        if (progressValue < 0) progressValue = 0
        if (progressValue > max) progressValue = max
        progress = progressValue
        onSizeChanged(width, height, 0, 0)
        onChangeListener?.onProgressChanged(this, progressValue, true)
    }
}

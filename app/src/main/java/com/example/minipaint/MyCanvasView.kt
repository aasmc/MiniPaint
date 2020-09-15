package com.example.minipaint

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs

private const val STROKE_WIDTH = 12f

class MyCanvasView(context: Context): View(context) {

    private var motionTouchEventX: Float = 0f
    private var motionTouchEventY: Float = 0f
    private var currentX: Float = 0f
    private var currentY: Float = 0f
    // this prevents the canvas from being redrawn even after some very tiny movements on the screen
    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    /**
     * Lateinit variables used for caching the drawn objects on the canvas
     */
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    private lateinit var frame: Rect

    private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)

    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)

    private val paint = Paint().apply {
        color = drawColor
        // Smooths out the edges of what is drawn on the canvas
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND // defines how drawn shapes are joined on the canvas. Default = MITER
        strokeCap = Paint.Cap.ROUND // defines how edges of drawn shapes are seen on the canvas. Default = BUTT
        strokeWidth = STROKE_WIDTH // default = Hairline-width (this is really thin)
    }

    private var path = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // new bitMap is created every time the method is called
        // It is a memory leak, because old bitmaps are left behind
        // to prevent that we recycle an already initialized bitmap
        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        // the third argument is the recommended configuration to store colors in 4-bit format (Alpha, Red, Green, Blue)
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)

        extraCanvas.drawColor(backgroundColor)

        val inset = 40
        // create the rectangle
        frame = Rect(inset, inset, width - inset, height - inset)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawBitmap(extraBitmap, 0f, 0f, null)

        // draw the rectangle frame on the canvas
        canvas?.drawRect(frame, paint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        motionTouchEventX = event!!.x
        motionTouchEventY = event.y

        when(event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }
        return true
    }

    private fun touchUp() {
        // reset the path so it doesn't get drawn again
        path.reset()
    }

    private fun touchMove() {
        // calculate the travelled distance
        val dx = abs(motionTouchEventX - currentX)
        val dy = abs(motionTouchEventY - currentY)
        // if the distance exceeds touchTolerance defined as the class variable
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // QuadTo() adds a quadratic bezier from the last point,
            // approaching control point(x1, y1) and ending at(x2, y2)
            // it creates a smoothly drawn line without corners
            path.quadTo(currentX, currentY, (motionTouchEventX + currentX) / 2, (motionTouchEventY + currentY) / 2)
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            // draw the path in the extraBitmap to cache it
            extraCanvas.drawPath(path, paint)
         }
        // call the method to force the canvas to redraw
        invalidate()
    }

    private fun touchStart() {
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }
}
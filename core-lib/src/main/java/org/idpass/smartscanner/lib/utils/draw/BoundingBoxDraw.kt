package org.idpass.smartscanner.lib.utils.draw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

class BoundingBoxDraw(context: Context, var rect: Rect): View(context) {

    private lateinit var boundaryPaint: Paint

    init {
        init()
    }

    private fun init() {
        boundaryPaint = Paint()
        boundaryPaint.color = Color.RED
        boundaryPaint.strokeWidth = 10f
        boundaryPaint.style = Paint.Style.STROKE
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawRect(rect.left.toFloat(), rect.top.toFloat(),  rect.right.toFloat(), rect.bottom.toFloat(), boundaryPaint)
    }
}
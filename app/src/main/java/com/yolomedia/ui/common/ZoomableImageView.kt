package com.yolomedia.ui.common

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrix_ = Matrix()
    private val savedMatrix = Matrix()
    private val startPoint = PointF()
    private val midPoint = PointF()

    private var mode = NONE
    private var minScale = 1f
    private var maxScale = 8f
    private var currentScale = 1f

    private val scaleDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector
    private val matrixValues = FloatArray(9)

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    init {
        scaleType = ScaleType.MATRIX
        isClickable = true

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                var scaleFactor = detector.scaleFactor
                matrix_.getValues(matrixValues)
                currentScale = matrixValues[Matrix.MSCALE_X]

                if ((currentScale * scaleFactor > maxScale && scaleFactor > 1f) ||
                    (currentScale * scaleFactor < minScale && scaleFactor < 1f)) {
                    return true
                }

                matrix_.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                constrainMatrix()
                imageMatrix = matrix_
                return true
            }
        })

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                matrix_.getValues(matrixValues)
                currentScale = matrixValues[Matrix.MSCALE_X]

                if (currentScale > minScale * 1.5f) {
                    resetToFit()
                } else {
                    val targetScale = 3f
                    val factor = targetScale / currentScale
                    matrix_.postScale(factor, factor, e.x, e.y)
                    constrainMatrix()
                    imageMatrix = matrix_
                }
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                return false
            }
        })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetToFit()
    }

    fun resetToFit() {
        val drawable = drawable ?: return
        val dWidth = drawable.intrinsicWidth.toFloat()
        val dHeight = drawable.intrinsicHeight.toFloat()
        val vWidth = width.toFloat()
        val vHeight = height.toFloat()

        if (dWidth <= 0 || dHeight <= 0 || vWidth <= 0 || vHeight <= 0) return

        val scale = minOf(vWidth / dWidth, vHeight / dHeight)
        minScale = scale

        matrix_.reset()
        matrix_.postScale(scale, scale)
        matrix_.postTranslate(
            (vWidth - dWidth * scale) / 2f,
            (vHeight - dHeight * scale) / 2f
        )
        imageMatrix = matrix_
        currentScale = scale
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix_)
                startPoint.set(event.x, event.y)
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                savedMatrix.set(matrix_)
                mode = ZOOM
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG && !scaleDetector.isInProgress) {
                    matrix_.set(savedMatrix)
                    val dx = event.x - startPoint.x
                    val dy = event.y - startPoint.y
                    matrix_.postTranslate(dx, dy)
                    constrainMatrix()
                    imageMatrix = matrix_
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }

        return true
    }

    private fun constrainMatrix() {
        val drawable = drawable ?: return
        val dWidth = drawable.intrinsicWidth.toFloat()
        val dHeight = drawable.intrinsicHeight.toFloat()
        val vWidth = width.toFloat()
        val vHeight = height.toFloat()

        matrix_.getValues(matrixValues)
        val scale = matrixValues[Matrix.MSCALE_X]
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]

        val scaledWidth = dWidth * scale
        val scaledHeight = dHeight * scale

        var dx = 0f
        var dy = 0f

        if (scaledWidth <= vWidth) {
            dx = (vWidth - scaledWidth) / 2f - transX
        } else {
            if (transX > 0) dx = -transX
            if (transX + scaledWidth < vWidth) dx = vWidth - transX - scaledWidth
        }

        if (scaledHeight <= vHeight) {
            dy = (vHeight - scaledHeight) / 2f - transY
        } else {
            if (transY > 0) dy = -transY
            if (transY + scaledHeight < vHeight) dy = vHeight - transY - scaledHeight
        }

        matrix_.postTranslate(dx, dy)
    }
}

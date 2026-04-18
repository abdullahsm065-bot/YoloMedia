package com.yolomedia.ui.common

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
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

    private val imageMatrix_ = Matrix()
    private val savedMatrix = Matrix()
    private val startPoint = PointF()

    private var mode = NONE
    private var minScale = 0.5f
    private var maxScale = 8f
    private var currentScale = 1f
    private var isInitialized = false

    private val scaleDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector
    private val matrixValues = FloatArray(9)

    private var activityToggle: (() -> Unit)? = null

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
                imageMatrix_.getValues(matrixValues)
                currentScale = matrixValues[Matrix.MSCALE_X]

                val newScale = currentScale * scaleFactor
                if (newScale > maxScale) scaleFactor = maxScale / currentScale
                if (newScale < minScale * 0.5f) scaleFactor = (minScale * 0.5f) / currentScale

                imageMatrix_.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                constrainMatrix()
                imageMatrix = imageMatrix_
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                imageMatrix_.getValues(matrixValues)
                currentScale = matrixValues[Matrix.MSCALE_X]
                if (currentScale < minScale) {
                    resetToFit()
                }
            }
        })

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                imageMatrix_.getValues(matrixValues)
                currentScale = matrixValues[Matrix.MSCALE_X]

                if (currentScale > minScale * 1.5f) {
                    resetToFit()
                } else {
                    val targetScale = minScale * 3f
                    val factor = targetScale / currentScale
                    imageMatrix_.postScale(factor, factor, e.x, e.y)
                    constrainMatrix()
                    imageMatrix = imageMatrix_
                    currentScale = targetScale
                }
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                activityToggle?.invoke()
                return true
            }
        })
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (drawable != null && width > 0 && height > 0) {
            post { resetToFit() }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            post { resetToFit() }
        }
    }

    fun setToggleControlsListener(listener: () -> Unit) {
        activityToggle = listener
    }

    fun resetToFit() {
        val d = drawable ?: return
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        val vw = width.toFloat()
        val vh = height.toFloat()

        if (dw <= 0 || dh <= 0 || vw <= 0 || vh <= 0) return

        val scale = minOf(vw / dw, vh / dh)
        minScale = scale

        imageMatrix_.reset()
        imageMatrix_.postScale(scale, scale)
        imageMatrix_.postTranslate(
            (vw - dw * scale) / 2f,
            (vh - dh * scale) / 2f
        )
        imageMatrix = imageMatrix_
        currentScale = scale
        isInitialized = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInitialized) return super.onTouchEvent(event)

        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(imageMatrix_)
                startPoint.set(event.x, event.y)
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                savedMatrix.set(imageMatrix_)
                mode = ZOOM
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG && !scaleDetector.isInProgress) {
                    imageMatrix_.getValues(matrixValues)
                    val cs = matrixValues[Matrix.MSCALE_X]
                    if (cs > minScale * 1.05f) {
                        imageMatrix_.set(savedMatrix)
                        val dx = event.x - startPoint.x
                        val dy = event.y - startPoint.y
                        imageMatrix_.postTranslate(dx, dy)
                        constrainMatrix()
                        imageMatrix = imageMatrix_
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }

        return true
    }

    private fun constrainMatrix() {
        val d = drawable ?: return
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        val vw = width.toFloat()
        val vh = height.toFloat()

        imageMatrix_.getValues(matrixValues)
        val scale = matrixValues[Matrix.MSCALE_X]
        val tx = matrixValues[Matrix.MTRANS_X]
        val ty = matrixValues[Matrix.MTRANS_Y]

        val sw = dw * scale
        val sh = dh * scale

        var dx = 0f
        var dy = 0f

        if (sw <= vw) {
            dx = (vw - sw) / 2f - tx
        } else {
            if (tx > 0) dx = -tx
            if (tx + sw < vw) dx = vw - tx - sw
        }

        if (sh <= vh) {
            dy = (vh - sh) / 2f - ty
        } else {
            if (ty > 0) dy = -ty
            if (ty + sh < vh) dy = vh - ty - sh
        }

        imageMatrix_.postTranslate(dx, dy)
    }
}

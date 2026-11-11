package com.ankit.jarvisfacescanner.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.ankit.jarvisfacescanner.ml.RecognitionResult

class JarvisReticleOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var results: List<RecognitionResult> = emptyList()
    private var sourceImageWidth = 480
    private var sourceImageHeight = 640

    private val boxPaintCyan = Paint().apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val boxPaintAlert = Paint().apply {
        color = Color.parseColor("#FF1744")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#E0F7FA")
        textSize = 34f
        typeface = Typeface.MONOSPACE
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val bgBadgePaint = Paint().apply {
        color = Color.parseColor("#CC050B14")
        style = Paint.Style.FILL
    }

    fun updateResults(newResults: List<RecognitionResult>, srcW: Int, srcH: Int) {
        this.results = newResults
        this.sourceImageWidth = srcW
        this.sourceImageHeight = srcH
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (sourceImageWidth == 0 || sourceImageHeight == 0) return

        val scaleX = width.toFloat() / sourceImageWidth
        val scaleY = height.toFloat() / sourceImageHeight

        for (res in results) {
            val paint = if (res.isKnown) boxPaintCyan else boxPaintAlert
            val left = res.boundingBox.left * scaleX
            val top = res.boundingBox.top * scaleY
            val right = res.boundingBox.right * scaleX
            val bottom = res.boundingBox.bottom * scaleY

            val cornerLen = (right - left) * 0.25f
            canvas.drawLine(left, top, left + cornerLen, top, paint)
            canvas.drawLine(left, top, left, top + cornerLen, paint)
            canvas.drawLine(right, top, right - cornerLen, top, paint)
            canvas.drawLine(right, top, right, top + cornerLen, paint)
            canvas.drawLine(left, bottom, left + cornerLen, bottom, paint)
            canvas.drawLine(left, bottom, left, bottom - cornerLen, paint)
            canvas.drawLine(right, bottom, right - cornerLen, bottom, paint)
            canvas.drawLine(right, bottom, right, bottom - cornerLen, paint)

            val label = if (res.isKnown) {
                "${res.matchedName} [SIM: ${(res.confidenceEstimate * 100).toInt()}%]"
            } else {
                "UNKNOWN PERSON [SIM: ${(res.confidenceEstimate * 100).toInt()}%]"
            }

            val badgeWidth = textPaint.measureText(label) + 20f
            val badgeHeight = 44f
            val badgeTop = if (top - badgeHeight > 0) top - badgeHeight - 10f else bottom + 10f

            canvas.drawRect(left, badgeTop, left + badgeWidth, badgeTop + badgeHeight, bgBadgePaint)
            canvas.drawText(label, left + 10f, badgeTop + 32f, textPaint)
        }
    }
}

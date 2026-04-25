package com.example.niord

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.widget.FrameLayout

class OnboardingSceneLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D9D9D9")
        style = Paint.Style.FILL
    }
    private val scenePath = Path()

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        buildScenePath()
        canvas.drawPath(scenePath, paint)
        super.onDraw(canvas)
    }

    override fun dispatchDraw(canvas: Canvas) {
        buildScenePath()
        val saveCount = canvas.save()
        canvas.clipPath(scenePath)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(saveCount)
    }

    private fun buildScenePath() {
        val curveDepth = 64f * resources.displayMetrics.density
        val curveStart = height - curveDepth

        scenePath.reset()
        scenePath.moveTo(0f, 0f)
        scenePath.lineTo(width.toFloat(), 0f)
        scenePath.lineTo(width.toFloat(), curveStart)
        scenePath.quadTo(width / 2f, height + curveDepth, 0f, curveStart)
        scenePath.close()
    }
}

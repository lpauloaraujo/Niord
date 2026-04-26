package com.example.niord

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max
import kotlin.math.min


fun dpToPx(context: Context, dpValue: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dpValue,
        context.resources.displayMetrics
    ).toInt()
}

fun dpToPxPair(context: Context, pair: Pair<Float, Float>): Pair<Int, Int> {
    return Pair(dpToPx(context, pair.first), dpToPx(context, pair.second))
}


fun intervalLimit(a: Int, value: Int, b: Int): Int {
    // a < val < b -> return val
    //val >= b -> return b
    //val <= a -> return a
    return max(a, min(value, b))
}

fun View.applyStatusBarPadding() {
    val initialPaddingTop = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        view.updatePadding(top = initialPaddingTop + statusBarTop)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

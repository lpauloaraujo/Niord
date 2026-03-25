package com.example.niord

import android.content.Context
import android.util.TypedValue
import kotlin.math.max
import kotlin.math.min


fun dpToPx(context: Context, dpValue: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dpValue,
        context.resources.displayMetrics
    ).toInt()
}


fun intervalLimit(a: Int, value: Int, b: Int): Int {
    // a < val < b -> return val
    //val > b -> return b
    //val < a -> return a
    return max(a, min(value, b))
}
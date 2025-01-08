package com.hvi2ist.chartlib.util

import android.content.res.Resources
import android.util.TypedValue

val Int.dp: Float
    get() {

        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        )
    }

val Int.dpInt: Int
    get() = dp.toInt()

val Int.sp: Float
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        )
    }

val Int.px: Float
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        )
    }

val Float.dp: Float
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this,
            Resources.getSystem().displayMetrics
        )
    }

val Float.sp: Float
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            this,
            Resources.getSystem().displayMetrics
        )
    }

val Float.px: Float
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            this,
            Resources.getSystem().displayMetrics
        )
    }

val Double.dp: Float
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        )
    }

val Double.sp: Float
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        )
    }

val Double.px: Float
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        )
    }
package dev.nash.dubbing.common

import android.content.Context
import android.util.TypedValue
import android.widget.TextView

object UiUtils {

    fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    fun createBody(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12))
        }
    }
}

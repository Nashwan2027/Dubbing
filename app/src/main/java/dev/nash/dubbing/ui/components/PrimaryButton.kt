package dev.nash.dubbing.ui.components

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatButton
import dev.nash.dubbing.R
import dev.nash.dubbing.common.UiUtils

class PrimaryButton(context: Context) : AppCompatButton(context) {

    init {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        isAllCaps = false

        setBackgroundColor(context.getColor(R.color.primary))
        setTextColor(context.getColor(R.color.white))

        setPadding(
            UiUtils.dp(context, 16),
            UiUtils.dp(context, 12),
            UiUtils.dp(context, 16),
            UiUtils.dp(context, 12)
        )
    }
}

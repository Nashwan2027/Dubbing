package dev.nash.dubbing.ui.components

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.widget.AppCompatButton
import dev.nash.dubbing.common.AppColors

class SecondaryButton(context: Context) : AppCompatButton(context) {
    init {
        setTextColor(AppColors.textPrimary)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18f
            setColor(AppColors.surfaceAlt)
            setStroke(2, AppColors.stroke)
        }
    }
}

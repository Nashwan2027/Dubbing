package dev.nash.dubbing.ui.components

import android.content.Context
import android.graphics.Color
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import dev.nash.dubbing.common.AppColors

class AppCardView(context: Context) : CardView(context) {
    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(24, 24, 24, 24)
    }

    init {
        radius = 24f
        setCardBackgroundColor(AppColors.surface)
        addView(container)
    }

    fun getContainer(): LinearLayout = container

    fun textPrimary(): Int = AppColors.textPrimary
    fun textSecondary(): Int = AppColors.textSecondary
    fun surface(): Int = AppColors.surface
    fun color(value: Int): Int = Color.valueOf(value).toArgb()
}

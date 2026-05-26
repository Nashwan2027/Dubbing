package dev.nash.dubbing.ui.components

import android.content.Context
import android.widget.TextView
import dev.nash.dubbing.common.AppColors

fun createBadge(context: Context, text: String): TextView {
    return TextView(context).apply {
        this.text = text
        textSize = 12f
        setTextColor(AppColors.textSecondary)
    }
}

fun createTitle(context: Context, text: String, onClick: (() -> Unit)? = null): TextView {
    return TextView(context).apply {
        this.text = text
        textSize = 20f
        setTextColor(AppColors.textPrimary)
        if (onClick != null) {
            setOnClickListener { onClick.invoke() }
        }
    }
}

fun createBody(context: Context, text: String): TextView {
    return TextView(context).apply {
        this.text = text
        textSize = 14f
        setTextColor(AppColors.textSecondary)
    }
}

fun createBody(context: Context, text: String, onClick: (() -> Unit)?): TextView {
    return createBody(context, text).apply {
        if (onClick != null) {
            setOnClickListener { onClick.invoke() }
        }
    }
}

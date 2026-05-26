package dev.nash.dubbing.ui.components.editor

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import dev.nash.dubbing.common.UiUtils

class EditorBottomBar(context: Context) : HorizontalScrollView(context) {

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(10, 10, 10, 10)
    }

    private val toolsList = mutableListOf<Pair<String, () -> Unit>>()

    init {
        isHorizontalScrollBarEnabled = false
        setBackgroundColor(Color.parseColor("#121212"))
        addView(container)
    }

    fun addTool(iconResId: Int, label: String, onClick: () -> Unit) {
        val toolView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(UiUtils.dp(context, 75), LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener { onClick() }
            
            val iconView = ImageView(context).apply {
                setImageResource(iconResId)
                layoutParams = LinearLayout.LayoutParams(UiUtils.dp(context, 26), UiUtils.dp(context, 26))
            }
            
            val labelView = TextView(context).apply { 
                text = label
                textSize = 11f
                setTextColor(Color.parseColor("#B0B0B0"))
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 0) 
            }
            
            addView(iconView)
            addView(labelView)
        }
        container.addView(toolView)
        toolsList.add(label to onClick)
    }

    fun getToolClickListener(label: String): (() -> Unit)? {
        return toolsList.find { it.first == label }?.second
    }
}

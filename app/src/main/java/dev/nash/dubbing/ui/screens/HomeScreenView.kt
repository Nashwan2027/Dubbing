package dev.nash.dubbing.ui.screens

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import dev.nash.dubbing.common.UiUtils
import dev.nash.dubbing.ui.components.AppCardView

class HomeScreenView(
    context: Context,
    private val onCreateProject: () -> Unit,
    private val onLoadProject: () -> Unit,
    private val onManageProjects: () -> Unit
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER // التوسيط بالكامل
        setBackgroundColor(Color.parseColor("#0A0A0C"))
        setPadding(UiUtils.dp(context, 20), UiUtils.dp(context, 20), UiUtils.dp(context, 20), UiUtils.dp(context, 20))

        val headerContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, UiUtils.dp(context, 40))
        }

        val header = TextView(context).apply {
            text = "Dubbing Studio"
            textSize = 32f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, UiUtils.dp(context, 10))
        }
        
        val subHeader = TextView(context).apply {
            text = "الذكاء الاصطناعي في خدمة إبداعك 🎙️"
            textSize = 16f
            setTextColor(Color.parseColor("#8E8E93"))
            gravity = Gravity.CENTER
        }

        headerContainer.addView(header)
        headerContainer.addView(subHeader)
        addView(headerContainer)

        val actionsContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        actionsContainer.addView(createActionCard(
            context, icon = "🎬", title = "إنشاء مشروع جديد", desc = "استيراد فيديو والبدء", bgColor = "#2E5BFF", onClick = onCreateProject
        ))

        actionsContainer.addView(View(context).apply { layoutParams = LayoutParams(1, UiUtils.dp(context, 16)) })

        actionsContainer.addView(createActionCard(
            context, icon = "📂", title = "فتح مشروع موجود", desc = "إكمال العمل على مشاريعك", bgColor = "#1E1E24", onClick = onLoadProject
        ))

        actionsContainer.addView(View(context).apply { layoutParams = LayoutParams(1, UiUtils.dp(context, 16)) })

        actionsContainer.addView(createActionCard(
            context, icon = "⚙️", title = "إدارة المشاريع", desc = "تعديل أو حذف الملفات", bgColor = "#1E1E24", onClick = onManageProjects
        ))

        addView(actionsContainer)
    }

    private fun createActionCard(
        context: Context, icon: String, title: String, desc: String, bgColor: String, onClick: () -> Unit
    ): AppCardView {
        val card = AppCardView(context).apply {
            setCardBackgroundColor(Color.parseColor(bgColor))
            radius = UiUtils.dp(context, 16).toFloat()
            cardElevation = UiUtils.dp(context, 8).toFloat()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setOnClickListener { onClick() }
        }

        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(UiUtils.dp(context, 20), UiUtils.dp(context, 20), UiUtils.dp(context, 20), UiUtils.dp(context, 20))
        }

        val iconView = TextView(context).apply {
            text = icon; textSize = 32f; setPadding(0, 0, UiUtils.dp(context, 20), 0)
        }

        val textContainer = LinearLayout(context).apply { orientation = VERTICAL }

        val titleView = TextView(context).apply {
            text = title; textSize = 18f; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD)
        }
        val descView = TextView(context).apply {
            text = desc; textSize = 12f; setTextColor(Color.parseColor("#D1D1D6")); setPadding(0, UiUtils.dp(context, 4), 0, 0)
        }

        textContainer.addView(titleView); textContainer.addView(descView)
        container.addView(iconView); container.addView(textContainer)
        card.addView(container)

        return card
    }
}

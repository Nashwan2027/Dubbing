package dev.nash.dubbing.ui.screens

import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView
import dev.nash.dubbing.common.AppColors
import dev.nash.dubbing.common.UiUtils
import dev.nash.dubbing.data.repository.ProjectRepository
import dev.nash.dubbing.ui.components.ProjectItemView
import dev.nash.dubbing.ui.components.PrimaryButton

class LoadProjectView(
    context: Context,
    private val repo: ProjectRepository,
    private val onProjectSelected: (String) -> Unit
) : LinearLayout(context) {

    private val refreshBtn = PrimaryButton(context).apply {
        text = "تحديث القائمة"
        setOnClickListener { reload() }
    }

    private val listContainer = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(UiUtils.dp(context, 16), UiUtils.dp(context, 12), UiUtils.dp(context, 16), UiUtils.dp(context, 16))
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(AppColors.background)

        addView(refreshBtn)
        addView(ScrollView(context).apply { addView(listContainer) }, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        reload()
    }

    fun reload() {
        listContainer.removeAllViews()
        val items = repo.getRecentProjects()
        if (items.isEmpty()) {
            listContainer.addView(UiUtils.createBody(context, "لا توجد مشاريع لتحميلها."))
            return
        }

        items.forEach { s ->
            // في شاشة التحميل: لا نعرض حذف (اختياري)
            val item = ProjectItemView(context).bind(
                project = s,
                onOpen = onProjectSelected,
                onDelete = null
            )
            listContainer.addView(item)
        }
    }
}

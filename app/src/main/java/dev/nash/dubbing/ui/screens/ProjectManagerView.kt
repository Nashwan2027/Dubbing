package dev.nash.dubbing.ui.screens

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import dev.nash.dubbing.common.AppColors
import dev.nash.dubbing.common.UiUtils
import dev.nash.dubbing.data.repository.ProjectRepository
import dev.nash.dubbing.ui.components.ProjectItemView
import dev.nash.dubbing.ui.components.PrimaryButton

class ProjectManagerView(
    context: Context,
    private val repo: ProjectRepository,
    private val onOpenProject: (String) -> Unit
) : LinearLayout(context) {

    private val createBtn = PrimaryButton(context).apply {
        text = "إنشاء مشروع جديد"
        setOnClickListener {
            showCreateProjectDialog(context)
        }
    }

    private val listContainer = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(UiUtils.dp(context, 16), UiUtils.dp(context, 12), UiUtils.dp(context, 16), UiUtils.dp(context, 16))
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(AppColors.background)

        addView(createBtn)

        addView(ScrollView(context).apply {
            addView(listContainer)
        }, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        reload()
    }

    private fun showCreateProjectDialog(context: Context) {
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(40, 30, 40, 30)
        }

        val nameInput = EditText(context).apply {
            hint = "اسم المشروع الجديد..."
            setSingleLine(true)
        }

        val descInput = EditText(context).apply {
            hint = "الوصف (اختياري)..."
            setSingleLine(true)
            setPadding(0, 30, 0, 0)
        }

        container.addView(nameInput)
        container.addView(descInput)

        AlertDialog.Builder(context)
            .setTitle("مشروع دبلجة جديد 🎥")
            .setView(container)
            .setPositiveButton("إنشاء") { dialog, _ ->
                val name = nameInput.text.toString().trim()
                val desc = descInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    val p = repo.createProject(name, desc)
                    Toast.makeText(context, "تم إنشاء: ${p.name}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    reload()
                } else {
                    Toast.makeText(context, "الرجاء إدخال اسم المشروع أولاً", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun reload() {
        listContainer.removeAllViews()
        val items = repo.getRecentProjects()
        if (items.isEmpty()) {
            listContainer.addView(UiUtils.createBody(context, "لا توجد مشاريع بعد."))
            return
        }

        items.forEach { s ->
            val item = ProjectItemView(context).bind(
                project = s,
                onOpen = onOpenProject,
                onDelete = { id ->
                    repo.deleteProject(id)
                    Toast.makeText(context, "تم حذف المشروع", Toast.LENGTH_SHORT).show()
                    reload()
                }
            )
            listContainer.addView(item)
        }
    }
}

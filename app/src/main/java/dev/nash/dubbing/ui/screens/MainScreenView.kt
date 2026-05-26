package dev.nash.dubbing.ui.screens

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import dev.nash.dubbing.data.repository.ProjectRepository

class MainScreenView(
    context: Context,
    private val repo: ProjectRepository,
    private val onCreateProject: (String) -> Unit,
    private val onLoadProject: () -> Unit,
    private val onManageProjects: () -> Unit
) : LinearLayout(context) {

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        
        // نعرض التصميم العصري الذي قمنا بإنشائه للتو
        val homeView = HomeScreenView(
            context = context,
            onCreateProject = { showCreateProjectDialog(context) },
            onLoadProject = onLoadProject,
            onManageProjects = onManageProjects
        )
        
        addView(homeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun showCreateProjectDialog(context: Context) {
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(50, 40, 50, 40)
        }

        val nameInput = EditText(context).apply {
            hint = "أدخل اسم المشروع الجديد..."
            setSingleLine(true)
        }

        val descInput = EditText(context).apply {
            hint = "وصف المشروع (اختياري)..."
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
                    onCreateProject(p.id)
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "الرجاء إدخال اسم المشروع أولاً", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

package dev.nash.dubbing.ui.components

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import dev.nash.dubbing.R
import dev.nash.dubbing.data.model.ProjectSummary

class ProjectItemView(context: Context) : LinearLayout(context) {

    private val nameView: TextView
    private val descView: TextView
    private val updatedView: TextView
    private val openBtn: Button
    private val deleteBtn: Button

    private var bound: ProjectSummary? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.project_item_view, this, true)
        nameView = findViewById(R.id.projectName)
        descView = findViewById(R.id.projectDescription)
        updatedView = findViewById(R.id.projectUpdatedAt)
        openBtn = findViewById(R.id.openButton)
        deleteBtn = findViewById(R.id.deleteButton)
    }

    fun bind(
        project: ProjectSummary,
        onOpen: (projectId: String) -> Unit,
        onDelete: ((projectId: String) -> Unit)? = null
    ): ProjectItemView {
        bound = project
        nameView.text = project.name
        descView.text = project.description
        updatedView.text = project.updatedAtLabel

        openBtn.setOnClickListener { onOpen(project.id) }

        if (onDelete == null) {
            deleteBtn.isEnabled = false
            deleteBtn.alpha = 0.4f
            deleteBtn.setOnClickListener(null)
        } else {
            deleteBtn.isEnabled = true
            deleteBtn.alpha = 1f
            deleteBtn.setOnClickListener {
                val p = bound ?: return@setOnClickListener
                AlertDialog.Builder(context)
                    .setTitle("تأكيد الحذف")
                    .setMessage("هل تريد حذف المشروع: ${p.name} ؟")
                    .setPositiveButton("حذف") { d, _ -> onDelete(p.id); d.dismiss() }
                    .setNegativeButton("إلغاء") { d, _ -> d.dismiss() }
                    .show()
            }
        }
        return this
    }
}

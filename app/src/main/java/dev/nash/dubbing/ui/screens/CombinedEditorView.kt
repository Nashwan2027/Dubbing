package dev.nash.dubbing.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.LinearLayout
import dev.nash.dubbing.data.repository.ProjectRepository
import dev.nash.dubbing.editor.tools.VideoEditorTools
import dev.nash.dubbing.ui.components.MediaSelectorView

class CombinedEditorView(
    context: Context,
    private val repository: ProjectRepository,
    private val tools: VideoEditorTools
) : LinearLayout(context) {

    private val mediaSelectorView = MediaSelectorView(context)

    init {
        orientation = VERTICAL
        addView(mediaSelectorView)
        mediaSelectorView.setOnMediaSelectedListener { uri: Uri? ->
            if (uri != null) {
                // إنشاء مشروع جديد عند اختيار ميديا
                repository.createProject("Imported Project")
                // لا نحتاج لإضافة مقطع هنا يدوياً، سيتم التعامل معه في EditorActivity
            }
        }
    }

    fun createNewProject(name: String = "New Project") {
        repository.createProject(name)
    }
}

package dev.nash.dubbing.ui.screens

import android.content.Context
import android.widget.LinearLayout
import dev.nash.dubbing.data.model.DubbingProject
import dev.nash.dubbing.ui.components.createTitle

class ProjectsScreenView(
    context: Context,
    private val projects: List<DubbingProject> = emptyList(),
    private val onOpen: (DubbingProject) -> Unit = {}
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        addView(createTitle(context, "Projects"))
        projects.forEach { project ->
            addView(createTitle(context, project.name) { onOpen(project) })
        }
    }
}

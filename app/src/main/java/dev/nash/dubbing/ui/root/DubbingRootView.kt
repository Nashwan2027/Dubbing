package dev.nash.dubbing.ui.root

import android.content.Context
import android.widget.FrameLayout
import dev.nash.dubbing.common.AppColors
import dev.nash.dubbing.data.repository.ProjectRepository
import dev.nash.dubbing.ui.screens.LoadProjectView
import dev.nash.dubbing.ui.screens.MainScreenView
import dev.nash.dubbing.ui.screens.ProjectManagerView

class DubbingRootView(
    context: Context,
    private val repo: ProjectRepository,
    private val openEditor: (projectId: String) -> Unit
) : FrameLayout(context) {

    private sealed class Screen {
        data object Home : Screen()
        data object Load : Screen()
        data object Manage : Screen()
    }

    private var current: Screen = Screen.Home

    init {
        setBackgroundColor(AppColors.background)
        render()
    }

    private fun render() {
        removeAllViews()
        when (current) {
            Screen.Home -> addView(
                MainScreenView(context, repo,
                    onCreateProject = { id -> openEditor(id) },
                    onLoadProject = { current = Screen.Load; render() },
                    onManageProjects = { current = Screen.Manage; render() }
                )
            )

            Screen.Load -> addView(
                LoadProjectView(context, repo) { id ->
                    openEditor(id)
                }
            )

            Screen.Manage -> addView(
                ProjectManagerView(context, repo) { id ->
                    openEditor(id)
                }
            )
        }
    }
}

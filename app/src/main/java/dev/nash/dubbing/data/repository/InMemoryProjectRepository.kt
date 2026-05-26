package dev.nash.dubbing.data.repository

import dev.nash.dubbing.data.model.DubbingProject
import dev.nash.dubbing.data.model.ProjectSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

open class InMemoryProjectRepository : ProjectRepository {
    private val projects = mutableListOf<DubbingProject>()
    private val dateFmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))

    override fun createProject(name: String, description: String): DubbingProject {
        val project = DubbingProject(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description
        )
        projects.add(project)
        return project
    }

    override fun loadAllProjects(): List<DubbingProject> = projects.toList()

    override fun saveProject(project: DubbingProject) {
        val index = projects.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            projects[index] = project.copy(updatedAt = System.currentTimeMillis())
        } else {
            projects.add(project)
        }
    }

    override fun getProjectById(id: String): DubbingProject? {
        return projects.find { it.id == id }
    }

    override fun getRecentProjects(): List<ProjectSummary> {
        return projects.sortedByDescending { it.updatedAt }.map { p ->
            ProjectSummary(
                id = p.id,
                name = p.name,
                description = p.description,
                updatedAtLabel = "آخر تعديل: ${dateFmt.format(Date(p.updatedAt))}"
            )
        }
    }

    override fun deleteProject(projectId: String) {
        projects.removeAll { it.id == projectId }
    }
}

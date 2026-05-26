package dev.nash.dubbing.data.repository

import dev.nash.dubbing.data.model.DubbingProject
import dev.nash.dubbing.data.model.ProjectSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ProjectRepositoryImpl(
    private val storage: ProjectStorageRepository
) : ProjectRepository {

    private val dateFmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.forLanguageTag("ar"))

    override fun createProject(name: String, description: String): DubbingProject {
        val now = System.currentTimeMillis()
        val project = DubbingProject(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            createdAt = now,
            updatedAt = now
        )
        storage.saveProject(project)
        return project
    }

    override fun loadAllProjects(): List<DubbingProject> {
        return storage.loadAllProjects()
    }

    override fun saveProject(project: DubbingProject) {
        storage.saveProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    override fun getProjectById(id: String): DubbingProject? {
        return storage.getProjectById(id)
    }

    override fun getRecentProjects(): List<ProjectSummary> {
        return storage.loadAllProjects()
            .sortedByDescending { it.updatedAt }
            .map { p ->
                ProjectSummary(
                    id = p.id,
                    name = p.name,
                    description = p.description,
                    updatedAtLabel = "آخر تعديل: ${dateFmt.format(Date(p.updatedAt))}"
                )
            }
    }

    override fun deleteProject(projectId: String) {
        storage.deleteProject(projectId)
    }
}

package dev.nash.dubbing.data.repository

import dev.nash.dubbing.data.model.DubbingProject
import dev.nash.dubbing.data.model.ProjectSummary

interface ProjectRepository {
    fun createProject(name: String = "مشروع جديد", description: String = ""): DubbingProject
    fun createDraftProject(name: String = "مسودة"): DubbingProject = createProject(name)
    fun loadAllProjects(): List<DubbingProject>
    fun saveProject(project: DubbingProject)
    fun getProjectById(id: String): DubbingProject?
    fun getRecentProjects(): List<ProjectSummary>
    fun deleteProject(projectId: String)
}

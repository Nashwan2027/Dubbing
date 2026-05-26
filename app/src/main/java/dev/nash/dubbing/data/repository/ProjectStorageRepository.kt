package dev.nash.dubbing.data.repository

import dev.nash.dubbing.data.model.DubbingProject

interface ProjectStorageRepository {
    fun getProjectById(projectId: String): DubbingProject?
    fun saveProject(project: DubbingProject)
    fun loadAllProjects(): List<DubbingProject>
    fun deleteProject(projectId: String)
}

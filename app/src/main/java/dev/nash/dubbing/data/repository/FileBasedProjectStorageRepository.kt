package dev.nash.dubbing.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.nash.dubbing.data.model.DubbingProject
import java.io.File
import java.util.UUID

class FileBasedProjectStorageRepository(
    private val context: Context
) : ProjectStorageRepository {

    private val gson = Gson()
    private val storageFile: File
        get() = File(context.filesDir, "projects.json")

    override fun loadAllProjects(): List<DubbingProject> {
        if (!storageFile.exists()) return emptyList()
        val json = storageFile.readText()
        if (json.isBlank()) return emptyList()
        val type = object : TypeToken<List<DubbingProject>>() {}.type
        return gson.fromJson<List<DubbingProject>>(json, type) ?: emptyList()
    }

    override fun saveProject(project: DubbingProject) {
        val projects = loadAllProjects().toMutableList()
        val index = projects.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            projects[index] = project
        } else {
            projects.add(project)
        }
        storageFile.writeText(gson.toJson(projects))
    }

    override fun getProjectById(projectId: String): DubbingProject? {
        return loadAllProjects().find { it.id == projectId }
    }

    override fun deleteProject(projectId: String) {
        val projects = loadAllProjects().toMutableList()
        projects.removeAll { it.id == projectId }
        storageFile.writeText(gson.toJson(projects))
    }
}

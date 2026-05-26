package dev.nash.dubbing.app

import android.content.Context
import dev.nash.dubbing.data.repository.FileBasedProjectStorageRepository
import dev.nash.dubbing.data.repository.ProjectRepository
import dev.nash.dubbing.data.repository.ProjectRepositoryImpl

class AppContainer(context: Context) {
    val projectRepository: ProjectRepository by lazy {
        // استخدام المستودع الفعلي الذي يدعم التخزين في ملفات
        ProjectRepositoryImpl(FileBasedProjectStorageRepository(context))
    }
}

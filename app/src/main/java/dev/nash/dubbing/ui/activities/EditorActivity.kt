package dev.nash.dubbing.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import dev.nash.dubbing.app.AppContainer
import dev.nash.dubbing.data.repository.ProjectRepository
import dev.nash.dubbing.editor.state.EditorState
import dev.nash.dubbing.editor.state.EditorStateStore
import dev.nash.dubbing.editor.tools.VideoEditorTools
import dev.nash.dubbing.ui.screens.EditorScreenView

class EditorActivity : AppCompatActivity() {
    private lateinit var store: EditorStateStore
    private lateinit var editorView: EditorScreenView
    private lateinit var repository: ProjectRepository
    private var projectId: String = ""

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (!uris.isNullOrEmpty()) {
            uris.forEach { takePermission(it) }
            editorView.setMultipleVideoUris(uris)
        }
    }

    private val pickSrtLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { takePermission(it); editorView.loadSrtFile(it) }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { takePermission(it); editorView.loadImage(it) }
    }

    private val pickWatermarkLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { takePermission(it); editorView.loadWatermark(it) }
    }

    private fun takePermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectId = intent.getStringExtra("projectId") ?: ""
        val appContainer = AppContainer(applicationContext)
        repository = appContainer.projectRepository

        store = EditorStateStore()
        val videoTools = VideoEditorTools(store)

        editorView = EditorScreenView(
            context = this,
            repository = repository,
            projectId = projectId,
            tools = videoTools,
            editorStateStore = store,
            onPickVideoRequest = { pickVideoLauncher.launch(arrayOf("video/*")) },
            onPickSrtRequest = { pickSrtLauncher.launch(arrayOf("*/*")) },
            onPickImageRequest = { pickImageLauncher.launch(arrayOf("image/*")) },
            onPickWatermarkRequest = { pickWatermarkLauncher.launch(arrayOf("image/*")) }
        )

        setContentView(editorView)
        loadSavedProject()
    }

    private fun loadSavedProject() {
        val project = repository.getProjectById(projectId)
        if (project != null) {
            store.updateState(
                EditorState(
                    clips = project.clips,
                    audioClips = project.audioClips,
                    subtitles = project.subtitles,
                    imageClips = project.imageClips,
                    watermark = project.watermark,
                    activeFilter = project.activeFilter,
                    subtitleStyle = project.subtitleStyle
                )
            )
            // تحديث الفيديو في المشغل إذا كان موجوداً
            if (project.clips.isNotEmpty()) {
                editorView.refreshVideoPlayer()
            }
        } else {
            Toast.makeText(this, "خطأ في تحميل المشروع", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        editorView.releasePlayer()
        super.onDestroy()
    }
}

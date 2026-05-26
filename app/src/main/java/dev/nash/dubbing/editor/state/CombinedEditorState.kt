package dev.nash.dubbing.editor.state

import dev.nash.dubbing.data.model.DubbingProject
import dev.nash.dubbing.data.model.EditorSelection

data class CombinedEditorState(
    val project: DubbingProject? = null,
    val videoSelection: EditorSelection = EditorSelection(),
    val audioSelection: EditorSelection = EditorSelection(),
    val playbackPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val statusMessage: String = "جاهز"
)

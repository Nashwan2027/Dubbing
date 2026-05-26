package dev.nash.dubbing.editor.state

import dev.nash.dubbing.data.model.DubbingProject

class CombinedEditorStateStore(
    initial: CombinedEditorState = CombinedEditorState()
) {
    private var state: CombinedEditorState = initial
    private val listeners = mutableListOf<(CombinedEditorState) -> Unit>()

    fun getState(): CombinedEditorState = state

    fun observe(listener: (CombinedEditorState) -> Unit) {
        listeners.add(listener)
        listener(state)
    }

    fun removeObserver(listener: (CombinedEditorState) -> Unit) {
        listeners.remove(listener)
    }

    fun setProject(project: DubbingProject) {
        update(state.copy(project = project, hasUnsavedChanges = false, statusMessage = "تم فتح المشروع"))
    }

    fun markDirty(message: String = "تم تعديل المشروع") {
        update(state.copy(hasUnsavedChanges = true, statusMessage = message))
    }

    private fun update(newState: CombinedEditorState) {
        state = newState
        val snap = state
        listeners.forEach { it(snap) }
    }
}

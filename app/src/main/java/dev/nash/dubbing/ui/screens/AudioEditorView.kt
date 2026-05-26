package dev.nash.dubbing.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.LinearLayout
import dev.nash.dubbing.editor.state.EditorStateStore

class AudioEditorView(
    context: Context,
    private val store: EditorStateStore
) : LinearLayout(context) {
    fun setAudioUri(uri: Uri?) {
    }
}

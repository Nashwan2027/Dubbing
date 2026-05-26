package dev.nash.dubbing.editor.state

import dev.nash.dubbing.data.model.AudioClip
import dev.nash.dubbing.data.model.ImageClip
import dev.nash.dubbing.data.model.SubtitleItem
import dev.nash.dubbing.data.model.VideoClip
import dev.nash.dubbing.data.model.WatermarkItem
import dev.nash.dubbing.data.model.SubtitleStyle

data class EditorState(
    val clips: List<VideoClip> = emptyList(),
    val audioClips: List<AudioClip> = emptyList(),
    val subtitles: List<SubtitleItem> = emptyList(),
    val imageClips: List<ImageClip> = emptyList(),
    val watermark: WatermarkItem? = null,
    val activeFilter: String = "none",
    val subtitleStyle: SubtitleStyle = SubtitleStyle() // تم إضافة هذا الحقل لحل الخطأ
)

class EditorStateStore {
    private var state = EditorState()
    private val listeners = mutableListOf<(EditorState) -> Unit>()

    fun getState(): EditorState = state

    fun observe(listener: (EditorState) -> Unit) {
        listeners.add(listener)
        listener(state)
    }

    fun removeObserver(listener: (EditorState) -> Unit) {
        listeners.remove(listener)
    }

    fun updateState(newState: EditorState) {
        state = newState
        listeners.forEach { it(state) }
    }
}

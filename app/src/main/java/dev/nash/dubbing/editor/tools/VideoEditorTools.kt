package dev.nash.dubbing.editor.tools

import dev.nash.dubbing.data.model.ImageClip
import dev.nash.dubbing.data.model.VideoClip
import dev.nash.dubbing.data.model.WatermarkItem
import dev.nash.dubbing.data.model.SubtitleStyle
import dev.nash.dubbing.editor.state.EditorStateStore
import java.util.Collections
import java.util.UUID

class VideoEditorTools(private val store: EditorStateStore) {

    // --- دالة مساعدة لإعادة حساب أوقات الفيديوهات بعد الترتيب أو الحذف ---
    private fun recalculateTimestamps(clips: List<VideoClip>): List<VideoClip> {
        var currentMs = 0L
        return clips.map { clip ->
            val duration = clip.endMs - clip.startMs
            val updatedClip = clip.copy(startMs = currentMs, endMs = currentMs + duration)
            currentMs += duration
            updatedClip
        }
    }

    fun deleteVideoClip(id: String) {
        val state = store.getState()
        val filtered = state.clips.filter { it.id != id }
        store.updateState(state.copy(clips = recalculateTimestamps(filtered)))
    }

    fun duplicateVideoClip(id: String) {
        val state = store.getState()
        val clipIndex = state.clips.indexOfFirst { it.id == id }
        if (clipIndex == -1) return
        val newClip = state.clips[clipIndex].copy(id = UUID.randomUUID().toString())
        val mutableClips = state.clips.toMutableList()
        mutableClips.add(clipIndex + 1, newClip) // إضافة النسخة بجوار الأصل
        store.updateState(state.copy(clips = recalculateTimestamps(mutableClips)))
    }

    // --- ميزة الترتيب لدمج الفيديوهات ---
    fun moveVideoClip(id: String, direction: Int) {
        val state = store.getState()
        val index = state.clips.indexOfFirst { it.id == id }
        if (index == -1) return
        
        val mutableClips = state.clips.toMutableList()
        val targetIndex = index + direction
        
        if (targetIndex >= 0 && targetIndex < mutableClips.size) {
            Collections.swap(mutableClips, index, targetIndex)
            store.updateState(state.copy(clips = recalculateTimestamps(mutableClips)))
        }
    }

    fun splitClipAt(timeMs: Long): Boolean {
        val state = store.getState()
        val targetIndex = state.clips.indexOfFirst { timeMs > it.startMs && timeMs < it.endMs }
        if (targetIndex == -1) return false
        val target = state.clips[targetIndex]
        val c1 = target.copy(id = UUID.randomUUID().toString(), endMs = timeMs)
        val c2 = target.copy(id = UUID.randomUUID().toString(), startMs = timeMs)
        val newClips = state.clips.toMutableList().apply { removeAt(targetIndex); add(targetIndex, c1); add(targetIndex + 1, c2) }
        store.updateState(state.copy(clips = recalculateTimestamps(newClips)))
        return true
    }

    fun deleteImageClip(id: String) {
        val state = store.getState()
        store.updateState(state.copy(imageClips = state.imageClips.filter { it.id != id }))
    }

    fun duplicateImageClip(id: String) {
        val state = store.getState()
        val clip = state.imageClips.find { it.id == id } ?: return
        val newClip = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = clip.timelineStartMs + clip.durationMs)
        store.updateState(state.copy(imageClips = state.imageClips + newClip))
    }

    fun updateImageDuration(id: String, newDuration: Long) {
        val state = store.getState()
        store.updateState(state.copy(imageClips = state.imageClips.map { if (it.id == id) it.copy(durationMs = newDuration) else it }))
    }

    fun updateImageClipPosition(id: String, newTime: Long) {
        val state = store.getState()
        store.updateState(state.copy(imageClips = state.imageClips.map { if (it.id == id) it.copy(timelineStartMs = newTime) else it }))
    }

    fun deleteSubtitle(id: String) {
        val state = store.getState()
        store.updateState(state.copy(subtitles = state.subtitles.filter { it.id != id }))
    }

    fun updateWatermark(item: WatermarkItem?) {
        store.updateState(store.getState().copy(watermark = item))
    }
    
    fun updateSubtitleStyle(style: SubtitleStyle) {
        store.updateState(store.getState().copy(subtitleStyle = style))
    }
}

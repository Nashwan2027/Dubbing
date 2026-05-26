package dev.nash.dubbing.editor.tools

import android.content.Context
import android.media.MediaMetadataRetriever
import dev.nash.dubbing.data.model.AudioClip
import dev.nash.dubbing.editor.state.EditorStateStore
import java.io.File
import java.util.UUID

class AudioEditorTools(private val store: EditorStateStore) {

    fun addAudioClip(uri: String, startMs: Long, endMs: Long, timelineStartMs: Long, type: String = "dubbing"): AudioClip {
        val clip = AudioClip(UUID.randomUUID().toString(), uri, startMs, endMs, timelineStartMs, 1.0f, type)
        val state = store.getState()
        store.updateState(state.copy(audioClips = state.audioClips + clip))
        return clip
    }

    fun addAudioFile(context: Context, audioFile: File, timelineStartMs: Long, type: String = "dubbing"): AudioClip? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, android.net.Uri.fromFile(audioFile))
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            retriever.release()
            
            addAudioClip(audioFile.absolutePath, 0L, duration, timelineStartMs, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteAudioClip(id: String) {
        val state = store.getState()
        store.updateState(state.copy(audioClips = state.audioClips.filter { it.id != id }))
    }

    fun duplicateAudioClip(id: String) {
        val state = store.getState()
        val clip = state.audioClips.find { it.id == id } ?: return
        val newClip = clip.copy(
            id = UUID.randomUUID().toString(), 
            timelineStartMs = clip.timelineStartMs + (clip.endMs - clip.startMs)
        )
        store.updateState(state.copy(audioClips = state.audioClips + newClip))
    }

    fun moveAudioClip(id: String, newTime: Long) {
        val state = store.getState()
        val updated = state.audioClips.map { if (it.id == id) it.copy(timelineStartMs = newTime) else it }
        store.updateState(state.copy(audioClips = updated))
    }
    
    fun updateAudioVolume(id: String, volume: Float) {
        val state = store.getState()
        val updated = state.audioClips.map { if (it.id == id) it.copy(volume = volume) else it }
        store.updateState(state.copy(audioClips = updated))
    }
}

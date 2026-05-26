package dev.nash.dubbing.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

class MediaMetadataHandler(private val context: Context) {

    fun getMediaInfo(mediaUri: Uri): MediaInfo? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, mediaUri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
            val format = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
            MediaInfo(
                uri = mediaUri,
                durationMs = duration ?: 0L,
                format = format ?: "unknown",
                width = width ?: 0,
                height = height ?: 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    data class MediaInfo(
        val uri: Uri,
        val durationMs: Long,
        val format: String,
        val width: Int,
        val height: Int
    )
}

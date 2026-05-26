package dev.nash.dubbing.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class MediaPlayerHandler(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun play(mediaUri: Uri) {
        stop()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, mediaUri)
            prepare()
            start()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

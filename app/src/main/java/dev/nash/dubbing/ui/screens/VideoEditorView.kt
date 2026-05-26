package dev.nash.dubbing.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.LinearLayout
import dev.nash.dubbing.media.preview.VideoPreviewView
import dev.nash.dubbing.media.player.VideoPlayer
import dev.nash.dubbing.common.UiUtils
import android.widget.Button

class VideoEditorView(context: Context) : LinearLayout(context) {

    private val videoPreview: VideoPreviewView = VideoPreviewView(context)

    private val playButton: Button = Button(context).apply {
        text = "تشغيل"
        setOnClickListener { videoPreview.play() }
    }

    private val pauseButton: Button = Button(context).apply {
        text = "إيقاف مؤقت"
        setOnClickListener { videoPreview.pause() }
    }

    private val stopButton: Button = Button(context).apply {
        text = "إيقاف"
        setOnClickListener { videoPreview.stop() }
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        
        addView(videoPreview)
        addView(playButton)
        addView(pauseButton)
        addView(stopButton)
    }

    fun setVideoUri(videoUri: Uri) {
        videoPreview.setVideoUri(videoUri)
    }

    fun releasePlayer() {
        videoPreview.release()
    }
}

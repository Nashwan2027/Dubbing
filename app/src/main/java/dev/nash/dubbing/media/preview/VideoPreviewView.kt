package dev.nash.dubbing.media.preview

import android.content.Context
import android.net.Uri
import android.widget.LinearLayout
import dev.nash.dubbing.media.player.VideoPlayer
import dev.nash.dubbing.common.UiUtils

class VideoPreviewView(context: Context) : LinearLayout(context) {

    val videoPlayer: VideoPlayer = VideoPlayer(context)

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            UiUtils.dp(context, 250)
        )
        addView(videoPlayer.playerView)
    }

    fun setVideoUri(videoUri: Uri) { videoPlayer.setMedia(videoUri) }
    fun play() { videoPlayer.play() }
    fun pause() { videoPlayer.pause() }
    fun stop() { videoPlayer.stop() } // تم إعادة الدالة هنا
    fun release() { videoPlayer.release() }
}

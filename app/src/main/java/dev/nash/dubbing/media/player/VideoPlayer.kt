package dev.nash.dubbing.media.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.Brightness
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dev.nash.dubbing.data.model.AudioClip
import dev.nash.dubbing.data.model.VideoClip
import android.os.Handler
import android.os.Looper

class VideoPlayer(private val context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    val playerView: PlayerView = PlayerView(context).apply {
        this.player = this@VideoPlayer.player
        useController = false
    }

    private var videoClips: List<VideoClip> = emptyList()
    private var audioClips: List<AudioClip> = emptyList()
    private val audioPlayersMap = mutableMapOf<String, ExoPlayer>()
    private val handler = Handler(Looper.getMainLooper())
    private var isSyncing = false

    private var globalVideoVolume = 1.0f
    private var globalDubbingVolume = 1.0f
    private var globalBgmVolume = 0.5f

    private val syncRunnable = object : Runnable {
        override fun run() {
            if (player.isPlaying && !isSyncing) {
                syncAudioClipsPlayback()
            }
            handler.postDelayed(this, 100)
        }
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    handler.post(syncRunnable)
                } else {
                    pauseAllAudioPlayers()
                }
            }
            
            override fun onPositionDiscontinuity(oldPos: Player.PositionInfo, newPos: Player.PositionInfo, reason: Int) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    seekAllAudioPlayersToMatch(currentPosition)
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    pauseAllAudioPlayers()
                }
            }
        })
    }

    fun setMedia(uri: Uri) {
        stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.volume = globalVideoVolume
    }

    fun setVideoClips(clips: List<VideoClip>) {
        this.videoClips = clips
        player.clearMediaItems()
        
        if (clips.isEmpty()) return
        
        clips.forEach { clip ->
            try {
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(clip.uri))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.startMs.coerceAtLeast(0))
                            .setEndPositionMs(clip.endMs.coerceAtLeast(clip.startMs + 1))
                            .build()
                    ).build()
                player.addMediaItem(mediaItem)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        player.prepare()
        player.volume = globalVideoVolume
    }

    fun setAudioClips(clips: List<AudioClip>) {
        val newIds = clips.map { it.id }.toSet()
        val iterator = audioPlayersMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!newIds.contains(entry.key)) {
                try {
                    entry.value.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                iterator.remove()
            }
        }
        
        this.audioClips = clips
        
        clips.forEach { clip ->
            try {
                val existing = audioPlayersMap[clip.id]
                val finalVolume = clip.volume * if (clip.type == "bgm") globalBgmVolume else globalDubbingVolume
                
                if (existing == null) {
                    val p = ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(Uri.parse(clip.uri)))
                        volume = finalVolume.coerceIn(0f, 1f)
                        prepare()
                    }
                    audioPlayersMap[clip.id] = p
                } else {
                    existing.volume = finalVolume.coerceIn(0f, 1f)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateMixerVolumes(video: Float, dubbing: Float, bgm: Float) {
        globalVideoVolume = video.coerceIn(0f, 1f)
        globalDubbingVolume = dubbing.coerceIn(0f, 1f)
        globalBgmVolume = bgm.coerceIn(0f, 1f)
        
        player.volume = globalVideoVolume
        
        audioClips.forEach { clip ->
            try {
                audioPlayersMap[clip.id]?.volume = (clip.volume * if (clip.type == "bgm") globalBgmVolume else globalDubbingVolume).coerceIn(0f, 1f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun applyVideoFilter(filterType: String) {
        val effectsList = mutableListOf<Effect>()
        when (filterType) {
            "grayscale" -> effectsList.add(RgbFilter.createGrayscaleFilter())
            "inverted" -> effectsList.add(RgbFilter.createInvertedFilter())
            "brightness" -> effectsList.add(Brightness(0.4f))
            "none" -> { }
        }
        try { 
            player.setVideoEffects(effectsList) 
        } catch (e: Exception) { 
            e.printStackTrace() 
        }
    }

    private fun syncAudioClipsPlayback() {
        if (isSyncing) return
        isSyncing = true
        
        try {
            val cur = currentPosition
            
            audioClips.forEach { clip ->
                try {
                    val p = audioPlayersMap[clip.id] ?: return@forEach
                    val clipDuration = clip.endMs - clip.startMs
                    val end = clip.timelineStartMs + clipDuration
                    
                    if (cur in clip.timelineStartMs..end) {
                        val expected = (cur - clip.timelineStartMs) + clip.startMs
                        val expectedSafe = expected.coerceIn(clip.startMs, clip.endMs)
                        
                        if (!p.isPlaying) { 
                            p.seekTo(expectedSafe)
                            p.play()
                        } else if (kotlin.math.abs(p.currentPosition - expectedSafe) > 50) {
                            p.seekTo(expectedSafe)
                        }
                    } else if (p.isPlaying) {
                        p.pause()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } finally {
            isSyncing = false
        }
    }

    private fun pauseAllAudioPlayers() {
        audioPlayersMap.values.forEach { 
            try {
                if (it.isPlaying) it.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun seekAllAudioPlayersToMatch(videoPos: Long) {
        audioClips.forEach { clip ->
            try {
                val p = audioPlayersMap[clip.id] ?: return@forEach
                val end = clip.timelineStartMs + (clip.endMs - clip.startMs)
                
                if (videoPos in clip.timelineStartMs..end) {
                    val seekPos = ((videoPos - clip.timelineStartMs) + clip.startMs).coerceIn(clip.startMs, clip.endMs)
                    p.seekTo(seekPos)
                    if (player.isPlaying && !p.isPlaying) p.play()
                } else if (p.isPlaying) {
                    p.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun play() { 
        player.playWhenReady = true 
    }
    
    fun pause() { 
        player.playWhenReady = false 
        pauseAllAudioPlayers()
    }
    
    fun stop() { 
        player.stop()
        pauseAllAudioPlayers()
    }
    
    fun release() {
        handler.removeCallbacks(syncRunnable)
        
        try {
            player.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        audioPlayersMap.values.forEach { 
            try {
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        audioPlayersMap.clear()
    }

    val isPlaying: Boolean get() = player.isPlaying

    val currentPosition: Long
        get() {
            if (videoClips.isEmpty()) return player.currentPosition.coerceAtLeast(0)
            var acc = 0L
            val idx = player.currentMediaItemIndex
            for (i in 0 until idx) {
                if (i < videoClips.size) {
                    acc += (videoClips[i].endMs - videoClips[i].startMs)
                }
            }
            return acc + player.currentPosition.coerceAtLeast(0)
        }

    val duration: Long
        get() = if (videoClips.isEmpty()) 0L else videoClips.sumOf { it.endMs - it.startMs }
}

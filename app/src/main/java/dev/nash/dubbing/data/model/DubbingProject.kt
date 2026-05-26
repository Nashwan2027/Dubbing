package dev.nash.dubbing.data.model

data class VideoClip(
    val id: String,
    val uri: String = "",
    val startMs: Long = 0L,
    val endMs: Long = 0L
)

data class AudioClip(
    val id: String,
    val uri: String = "",
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val timelineStartMs: Long = 0L,
    val volume: Float = 1.0f,
    val type: String = "dubbing"
)

data class ImageClip(
    val id: String,
    val uri: String,
    val timelineStartMs: Long,
    val durationMs: Long = 3000L,
    val opacity: Float = 1.0f,
    val scale: Float = 1.0f,
    val xPercent: Float = 0.5f,
    val yPercent: Float = 0.5f
)

data class WatermarkItem(
    val id: String = "default_watermark",
    val uri: String = "",
    val type: String = "image",
    val xPercent: Float = 0.9f,
    val yPercent: Float = 0.9f,
    val opacity: Float = 0.8f,
    val scale: Float = 1.0f,
    val text: String = ""
)

data class SubtitleStyle(
    val fontSize: Int = 22,
    val fontColor: String = "&H00FFFFFF", // أبيض
    val outlineColor: String = "&H00000000",
    val borderStyle: Int = 3,
    val alignment: Int = 2
)

data class DubbingProject(
    val id: String,
    val name: String,
    val description: String = "",
    val clips: List<VideoClip> = emptyList(),
    val audioClips: List<AudioClip> = emptyList(),
    val imageClips: List<ImageClip> = emptyList(),
    val watermark: WatermarkItem? = null,
    val subtitles: List<SubtitleItem> = emptyList(),
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val activeFilter: String = "none",
    val createdAt: Long = System.currentTimeMillis(), // تم إعادة هذا الحقل
    val updatedAt: Long = System.currentTimeMillis()
)

package dev.nash.dubbing.ui.components.editor

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import dev.nash.dubbing.common.UiUtils
import dev.nash.dubbing.data.model.AudioClip
import dev.nash.dubbing.data.model.VideoClip
import dev.nash.dubbing.media.preview.ThumbnailLoader
import kotlin.math.max

class DubbingTimelineView(context: Context) : View(context) {
    private var durationMs: Long = 0L
    private var currentTimeMs: Long = 0L
    private var videoClips: List<VideoClip> = emptyList()
    private var audioClips: List<AudioClip> = emptyList()
    private var subtitlesList: List<dev.nash.dubbing.data.model.SubtitleItem> = emptyList()
    private var imageClips: List<dev.nash.dubbing.data.model.ImageClip> = emptyList()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0A0A0C") }
    private val videoTrackBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E1E24") }
    private val videoBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4A4A5A"); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val selectionBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD60A"); style = Paint.Style.STROKE; strokeWidth = 6f }
    private val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF2D55"); strokeWidth = 4f }
    private val timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8E8E93"); textSize = 20f }
    private val addBtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2E5BFF") }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 30f; textAlign = Paint.Align.CENTER }

    private val trackHeight = UiUtils.dp(context, 50)
    private val trackSpacing = UiUtils.dp(context, 10)
    private val topPadding = UiUtils.dp(context, 40)
    private var pixelsPerMs = 0.1f
    private var zoomScale = 1.0f
    private var lastTouchX = 0f
    
    private var selectedClipType: ClipType? = null
    private var selectedClipId: String? = null
    private var dragStartOffset: Long = 0L

    var onSeekRequested: ((Long) -> Unit)? = null
    var onAddVideoClicked: (() -> Unit)? = null
    var onClipSelected: ((ClipType, String) -> Unit)? = null
    var onClipPositionChanged: ((ClipType, String, Long) -> Unit)? = null

    private val addVideoRect = RectF()

    enum class ClipType { VIDEO, AUDIO, SUBTITLE, IMAGE }

    fun setVideoAndAudioData(duration: Long, clips: List<VideoClip>, audios: List<AudioClip>) {
        this.durationMs = duration; this.videoClips = clips; this.audioClips = audios
        updateScaling(); invalidate()
    }

    fun setSubtitleAndImageData(subs: List<dev.nash.dubbing.data.model.SubtitleItem>, imgs: List<dev.nash.dubbing.data.model.ImageClip>) {
        this.subtitlesList = subs; this.imageClips = imgs; invalidate()
    }

    fun updatePlaybackTime(time: Long) { this.currentTimeMs = time; invalidate() }
    fun setZoom(scale: Float) { this.zoomScale = scale.coerceIn(0.5f, 5f); updateScaling(); invalidate() }
    fun getZoom() = zoomScale
    private fun updateScaling() { pixelsPerMs = (width.toFloat() / max(5000L, durationMs)) * zoomScale }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val center = width / 2f
        var currentTop = topPadding.toFloat()

        // مسار الفيديو
        var lastX = center - (currentTimeMs * pixelsPerMs)
        videoClips.forEach { clip ->
            val left = center + (clip.startMs - currentTimeMs) * pixelsPerMs
            val right = center + (clip.endMs - currentTimeMs) * pixelsPerMs
            val rect = RectF(left, currentTop, right, currentTop + trackHeight)
            canvas.drawRoundRect(rect, 10f, 10f, videoTrackBgPaint)
            canvas.drawRoundRect(rect, 10f, 10f, videoBorderPaint)
            if (selectedClipType == ClipType.VIDEO && selectedClipId == clip.id) canvas.drawRoundRect(rect, 10f, 10f, selectionBorderPaint)
            lastX = max(lastX, right)
        }
        
        // زر إضافة فيديو
        addVideoRect.set(lastX + 10f, currentTop, lastX + 100f, currentTop + trackHeight)
        canvas.drawRoundRect(addVideoRect, 10f, 10f, addBtnPaint)
        canvas.drawText("+", addVideoRect.centerX(), addVideoRect.centerY() + 10f, textPaint)

        // المسارات الأخرى (تبسيط للرسم)
        currentTop += trackHeight + trackSpacing
        subtitlesList.forEach { drawClip(canvas, center, it.id, it.startMs, it.endMs, currentTop, Color.parseColor("#FF9F0A"), ClipType.SUBTITLE) }
        currentTop += trackHeight + trackSpacing
        imageClips.forEach { drawClip(canvas, center, it.id, it.timelineStartMs, it.timelineStartMs + it.durationMs, currentTop, Color.parseColor("#32D74B"), ClipType.IMAGE) }
        currentTop += trackHeight + trackSpacing
        audioClips.forEach { drawClip(canvas, center, it.id, it.timelineStartMs, it.timelineStartMs + (it.endMs - it.startMs), currentTop, Color.parseColor("#2E5BFF"), ClipType.AUDIO) }

        // المؤشر
        canvas.drawLine(center, 0f, center, height.toFloat(), playheadPaint)
    }

    private fun drawClip(canvas: Canvas, center: Float, id: String, start: Long, end: Long, top: Float, color: Int, type: ClipType) {
        val left = center + (start - currentTimeMs) * pixelsPerMs
        val right = center + (end - currentTimeMs) * pixelsPerMs
        if (right < 0 || left > width) return
        val rect = RectF(left, top, right, top + trackHeight)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(rect, 10f, 10f, p)
        if (selectedClipType == type && selectedClipId == id) canvas.drawRoundRect(rect, 10f, 10f, selectionBorderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val center = width / 2f
        val touchTime = currentTimeMs + ((event.x - center) / pixelsPerMs).toLong()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                if (addVideoRect.contains(event.x, event.y)) { onAddVideoClicked?.invoke(); return true }
                checkSelection(event.y, touchTime)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (selectedClipId != null && selectedClipType != ClipType.VIDEO) {
                    val newPos = (touchTime + dragStartOffset).coerceAtLeast(0L)
                    onClipPositionChanged?.invoke(selectedClipType!!, selectedClipId!!, newPos)
                } else {
                    val dx = event.x - lastTouchX
                    currentTimeMs = (currentTimeMs - (dx / pixelsPerMs).toLong()).coerceIn(0, durationMs + 1000)
                    onSeekRequested?.invoke(currentTimeMs)
                }
                lastTouchX = event.x; invalidate(); return true
            }
            MotionEvent.ACTION_UP -> { selectedClipId = null; invalidate(); performClick(); return true }
        }
        return true
    }

    private fun checkSelection(y: Float, time: Long) {
        var top = topPadding.toFloat()
        // Video Track
        if (y in top..top+trackHeight) {
            videoClips.find { time in it.startMs..it.endMs }?.let { selectedClipId = it.id; selectedClipType = ClipType.VIDEO; onClipSelected?.invoke(ClipType.VIDEO, it.id) }
        }
        // Subtitle
        top += trackHeight + trackSpacing
        if (y in top..top+trackHeight) {
            subtitlesList.find { time in it.startMs..it.endMs }?.let { selectedClipId = it.id; selectedClipType = ClipType.SUBTITLE; onClipSelected?.invoke(ClipType.SUBTITLE, it.id) }
        }
        // Image
        top += trackHeight + trackSpacing
        if (y in top..top+trackHeight) {
            imageClips.find { time in it.timelineStartMs..it.timelineStartMs + it.durationMs }?.let { selectedClipId = it.id; selectedClipType = ClipType.IMAGE; onClipSelected?.invoke(ClipType.IMAGE, it.id) }
        }
    }

    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { super.onSizeChanged(w, h, oldw, oldh); updateScaling() }
}

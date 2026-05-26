package dev.nash.dubbing.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import dev.nash.dubbing.R
import dev.nash.dubbing.RecordingService
import dev.nash.dubbing.ai.EdgeTtsEngine
import dev.nash.dubbing.ai.SystemTtsEngine
import dev.nash.dubbing.data.model.*
import dev.nash.dubbing.data.repository.ProjectRepository
import dev.nash.dubbing.editor.state.EditorStateStore
import dev.nash.dubbing.editor.tools.VideoEditorTools
import dev.nash.dubbing.editor.tools.AudioEditorTools
import dev.nash.dubbing.editor.tools.SrtParser
import dev.nash.dubbing.editor.export.ExportEngine
import dev.nash.dubbing.media.preview.VideoPreviewView
import dev.nash.dubbing.ui.components.editor.DubbingTimelineView
import dev.nash.dubbing.ui.components.editor.EditorBottomBar
import dev.nash.dubbing.common.UiUtils
import java.io.File
import java.util.UUID

class EditorScreenView(
    context: Context,
    private val repository: ProjectRepository,
    private val projectId: String,
    private val tools: VideoEditorTools,
    private val editorStateStore: EditorStateStore,
    private val onPickVideoRequest: () -> Unit,
    private val onPickSrtRequest: () -> Unit,
    private val onPickImageRequest: () -> Unit,
    private val onPickWatermarkRequest: () -> Unit
) : FrameLayout(context) {

    private val rootLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val videoPreview = VideoPreviewView(context)
    private val interactiveTimeline = DubbingTimelineView(context)
    private val audioTools = AudioEditorTools(editorStateStore)
    
    private val edgeTts = EdgeTtsEngine()
    private var localTts: SystemTtsEngine = SystemTtsEngine(context)

    // طبقات العرض (Overlays) بأبعاد مضبوطة
    private val subtitleOverlay = TextView(context).apply { 
        setTextColor(Color.WHITE); visibility = GONE; gravity = Gravity.CENTER
        setBackgroundColor(Color.parseColor("#80000000")); setPadding(20, 10, 20, 10)
    }
    private val imageOverlay = ImageView(context).apply { visibility = GONE; scaleType = ImageView.ScaleType.FIT_CENTER }
    private val watermarkOverlay = ImageView(context).apply { visibility = GONE; scaleType = ImageView.ScaleType.FIT_CENTER }
    
    private val timeText = TextView(context).apply { setTextColor(Color.WHITE); textSize = 14f }
    private val playPauseBtn = ImageView(context).apply { 
        setImageResource(android.R.drawable.ic_media_play); setColorFilter(Color.WHITE); setPadding(20, 20, 20, 20) 
    }
    
    private val progressOverlay = FrameLayout(context).apply { 
        setBackgroundColor(Color.parseColor("#AA000000")); visibility = GONE
        addView(ProgressBar(context).apply { layoutParams = LayoutParams(-2, -2, Gravity.CENTER) })
    }

    private var pendingVideoInsertIndex: Int = -1

    init {
        setBackgroundColor(Color.parseColor("#0A0A0C"))
        localTts.init { }
        
        // حاوية العرض (مضبوطة الأبعاد لمنع الانهيار)
        val playerContainer = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            addView(videoPreview, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            
            // الترجمة في الأسفل
            addView(subtitleOverlay, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = 80 })
            
            // الصورة بحجم محدود في المنتصف
            val imgSize = UiUtils.dp(context, 150)
            addView(imageOverlay, LayoutParams(imgSize, imgSize, Gravity.CENTER))
            
            // العلامة المائية في الزاوية بحجم صغير
            val wmSize = UiUtils.dp(context, 80)
            addView(watermarkOverlay, LayoutParams(wmSize, wmSize, Gravity.TOP or Gravity.START).apply { topMargin = 20; leftMargin = 20 })
        }

        val controlBar = LinearLayout(context).apply { 
            gravity = Gravity.CENTER; setPadding(20, 10, 20, 10); setBackgroundColor(Color.parseColor("#121212"))
            addView(timeText); addView(playPauseBtn) 
        }

        // استعادة جميع الأدوات المفقودة
        val bottomBar = EditorBottomBar(context).apply {
            addTool(R.drawable.ic_cut, "قص") { showCutDialog() }
            addTool(R.drawable.ic_music, "فيديو") { pendingVideoInsertIndex = -1; onPickVideoRequest() }
            addTool(R.drawable.ic_image, "صورة") { onPickImageRequest() }
            addTool(R.drawable.ic_ai_magic, "العلامة") { editorStateStore.getState().watermark?.let { showWatermarkSettings(it) } ?: onPickWatermarkRequest() }
            addTool(R.drawable.ic_subtitles, "ترجمة") { onPickSrtRequest() }
            addTool(R.drawable.ic_subtitles, "تنسيق") { showSubtitleStyleDialog() }
            addTool(R.drawable.ic_ai_magic, "دبلجة") { showDubbingSelection() }
            addTool(R.drawable.ic_mixer_new, "مزج") { showMixerDialog() }
            addTool(R.drawable.ic_mic, "تسجيل") { showRecordingDialog() }
            addTool(android.R.drawable.ic_menu_zoom, "تكبير") { showZoomDialog() }
            addTool(R.drawable.ic_filter, "فلتر") { showFilterDialog() }
            addTool(R.drawable.ic_export, "تصدير") { startExport() }
            addTool(R.drawable.ic_export, "دمج سريع") { quickMerge() }
        }

        // التوزيع في الشاشة
        rootLayout.addView(playerContainer, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        rootLayout.addView(controlBar, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        rootLayout.addView(interactiveTimeline, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, UiUtils.dp(context, 200)))
        rootLayout.addView(bottomBar, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        
        addView(rootLayout)
        addView(progressOverlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        setupListeners()
        observeState()
        
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable { override fun run() { updateUI(); handler.postDelayed(this, 50) } })
    }

    private fun setupListeners() {
        playPauseBtn.setOnClickListener { 
            if (videoPreview.videoPlayer.isPlaying) videoPreview.pause() else videoPreview.play() 
            playPauseBtn.setImageResource(if (videoPreview.videoPlayer.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
        }
        
        interactiveTimeline.onSeekRequested = { videoPreview.videoPlayer.player.seekTo(it) }
        interactiveTimeline.onAddVideoClicked = { pendingVideoInsertIndex = -1; onPickVideoRequest() }
        interactiveTimeline.onClipPositionChanged = { type, id, time ->
            when(type) {
                DubbingTimelineView.ClipType.IMAGE -> tools.updateImageClipPosition(id, time)
                DubbingTimelineView.ClipType.AUDIO -> audioTools.moveAudioClip(id, time)
                else -> {}
            }
            saveCurrentState()
        }
        interactiveTimeline.onClipSelected = { type, id ->
            when(type) {
                DubbingTimelineView.ClipType.VIDEO -> showVideoActions(id)
                DubbingTimelineView.ClipType.SUBTITLE -> showSubtitleActions(id)
                else -> showGeneralActions(type, id)
            }
        }
    }

    // ==========================================
    // نوافذ الحوار والإجراءات (التي كانت مفقودة)
    // ==========================================

    private fun showVideoActions(id: String) {
        val state = editorStateStore.getState()
        val clipIndex = state.clips.indexOfFirst { it.id == id }
        
        val options = arrayOf("تكرار المقطع", "حذف المقطع", "تحريك للبداية", "تحريك للنهاية", "إضافة فيديو قبله", "إضافة فيديو بعده", "تطبيق فلتر")
        AlertDialog.Builder(context).setTitle("خيارات الفيديو").setItems(options) { _, w ->
            when(w) {
                0 -> tools.duplicateVideoClip(id)
                1 -> tools.deleteVideoClip(id)
                2 -> tools.moveVideoClip(id, -1)
                3 -> tools.moveVideoClip(id, 1)
                4 -> { pendingVideoInsertIndex = clipIndex; onPickVideoRequest() }
                5 -> { pendingVideoInsertIndex = clipIndex + 1; onPickVideoRequest() }
                6 -> showFilterDialog()
                3 -> { pendingVideoInsertIndex = clipIndex + 1; onPickVideoRequest() }
                4 -> showFilterDialog()
            }
            if (w in 0..3 || w == 6) { saveCurrentState(); refreshVideoPlayer() }
        }.show()
    }

    private fun showGeneralActions(type: DubbingTimelineView.ClipType, id: String) {
        AlertDialog.Builder(context).setTitle("خيارات").setItems(arrayOf("تكرار", "حذف", "تعديل المدة")) { _, w ->
            if (w == 0) {
                if (type == DubbingTimelineView.ClipType.IMAGE) tools.duplicateImageClip(id)
                else if (type == DubbingTimelineView.ClipType.AUDIO) audioTools.duplicateAudioClip(id)
            } else if (w == 1) {
                if (type == DubbingTimelineView.ClipType.IMAGE) tools.deleteImageClip(id)
                else if (type == DubbingTimelineView.ClipType.AUDIO) audioTools.deleteAudioClip(id)
            } else if (w == 2 && type == DubbingTimelineView.ClipType.IMAGE) {
                showDurationDialog(id)
            }
            saveCurrentState()
        }.show()
    }

    private fun showSubtitleActions(id: String) {
        AlertDialog.Builder(context).setTitle("الترجمة").setItems(arrayOf("حذف")) { _, _ ->
            tools.deleteSubtitle(id); saveCurrentState()
        }.show()
    }

    private fun showCutDialog() {
        val currentPos = videoPreview.videoPlayer.currentPosition
        AlertDialog.Builder(context).setTitle("قص الفيديو").setMessage("هل تريد قص الفيديو عند هذه النقطة؟")
            .setPositiveButton("قص") { _, _ ->
                if (tools.splitClipAt(currentPos)) { refreshVideoPlayer(); saveCurrentState() }
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun showFilterDialog() {
        val filters = arrayOf("بدون فلتر", "أبيض وأسود", "مقلوب")
        AlertDialog.Builder(context).setTitle("الفلاتر").setItems(filters) { _, w ->
            val f = when(w) { 1 -> "grayscale"; 2 -> "inverted"; else -> "none" }
            videoPreview.videoPlayer.applyVideoFilter(f)
            editorStateStore.updateState(editorStateStore.getState().copy(activeFilter = f))
            saveCurrentState()
        }.show()
    }

    private fun showZoomDialog() {
        val sb = SeekBar(context).apply { max = 200; progress = (interactiveTimeline.getZoom()*100).toInt() }
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) { interactiveTimeline.setZoom(p/100f) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        AlertDialog.Builder(context).setTitle("تكبير المخطط").setView(sb).show()
    }

    private fun showDurationDialog(id: String) {
        val input = EditText(context).apply { setRawInputType(android.text.InputType.TYPE_CLASS_NUMBER); hint = "المدة بالملي ثانية" }
        AlertDialog.Builder(context).setTitle("تعديل المدة").setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val dur = input.text.toString().toLongOrNull() ?: 3000L
                tools.updateImageDuration(id, dur); saveCurrentState()
            }.show()
    }

    private fun showSubtitleStyleDialog() {
        val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 50, 50, 50) }
        layout.addView(TextView(context).apply { text = "حجم الخط"; setTextColor(Color.WHITE) })
        val sizeSeek = SeekBar(context).apply { max = 50; progress = subtitleOverlay.textSize.toInt() }
        layout.addView(sizeSeek)

        val colors = arrayOf("أبيض", "أصفر", "أحمر", "أخضر")
        val colorHex = arrayOf("&H00FFFFFF", "&H0000FFFF", "&H000000FF", "&H0000FF00")
        val colorCodes = arrayOf(Color.WHITE, Color.YELLOW, Color.RED, Color.GREEN)

        layout.addView(TextView(context).apply { text = "\nلون الخط"; setTextColor(Color.WHITE) })
        val colorSpinner = Spinner(context).apply { adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, colors) }
        layout.addView(colorSpinner)

        AlertDialog.Builder(context).setTitle("تنسيق الترجمة").setView(layout)
            .setPositiveButton("تطبيق") { _, _ ->
                subtitleOverlay.textSize = sizeSeek.progress.toFloat()
                subtitleOverlay.setTextColor(colorCodes[colorSpinner.selectedItemPosition])
                val currentProject = repository.getProjectById(projectId)
                if (currentProject != null) {
                    val newStyle = SubtitleStyle(fontSize = sizeSeek.progress, fontColor = colorHex[colorSpinner.selectedItemPosition])
                    repository.saveProject(currentProject.copy(subtitleStyle = newStyle))
                }
            }.show()
    }

    private fun showWatermarkSettings(wm: WatermarkItem) {
        val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40) }
        var cx = wm.xPercent; var cy = wm.yPercent; var cs = wm.scale; var ca = wm.opacity
        val update = { tools.updateWatermark(wm.copy(xPercent = cx, yPercent = cy, scale = cs, opacity = ca)); saveCurrentState() }
        val addSeek = { t: String, i: Int, m: Int, op: (Int) -> Unit ->
            layout.addView(TextView(context).apply { text = t; setTextColor(Color.WHITE) })
            layout.addView(SeekBar(context).apply { max = m; progress = i; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) { op(p); update() }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })})
        }
        addSeek("الموضع X", (cx*100).toInt(), 100) { cx = it/100f }; addSeek("الموضع Y", (cy*100).toInt(), 100) { cy = it/100f }
        addSeek("الحجم", (cs*100).toInt(), 200) { cs = it/100f }; addSeek("الشفافية", (ca*100).toInt(), 100) { ca = it/100f }
        AlertDialog.Builder(context).setTitle("إعدادات العلامة").setView(layout).setPositiveButton("إغلاق", null).setNeutralButton("حذف") { _,_ -> tools.updateWatermark(null); saveCurrentState() }.show()
    }

    private fun showMixerDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.audio_mixer_dialog, null)
        val vSeek = dialogView.findViewById<SeekBar>(R.id.videoVolumeSeek)
        val dSeek = dialogView.findViewById<SeekBar>(R.id.dubbingVolumeSeek)
        val bSeek = dialogView.findViewById<SeekBar>(R.id.bgmVolumeSeek)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialogView.findViewById<Button>(R.id.mixerSaveBtn).setOnClickListener {
            videoPreview.videoPlayer.updateMixerVolumes(vSeek.progress/100f, dSeek.progress/100f, bSeek.progress/100f)
            dialog.dismiss()
        }
        dialogView.findViewById<Button>(R.id.mixerCancelBtn).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showRecordingDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.recording_dialog, null)
        val startBtn = dialogView.findViewById<Button>(R.id.recordStartBtn)
        val stopBtn = dialogView.findViewById<Button>(R.id.recordStopBtn)
        val saveBtn = dialogView.findViewById<Button>(R.id.recordingSaveBtn)
        
        val dialog = AlertDialog.Builder(context).setView(dialogView).setCancelable(false).create()
        var recordedFile: File? = null

        startBtn.setOnClickListener {
            val intent = Intent(context, RecordingService::class.java)
            context.startService(intent)
            context.bindService(intent, object : android.content.ServiceConnection {
                override fun onServiceConnected(n: android.content.ComponentName?, s: IBinder?) {
                    val service = (s as RecordingService.LocalBinder).getService()
                    val file = RecordingService.getRecordingFile(context)
                    service.setRecordingListener(object : RecordingService.RecordingListener {
                        override fun onRecordingStarted(f: File) { recordedFile = f; startBtn.isEnabled = false; stopBtn.isEnabled = true }
                        override fun onRecordingStopped(f: File, d: Long) { saveBtn.isEnabled = true; stopBtn.isEnabled = false }
                        override fun onRecordingError(e: String) { Toast.makeText(context, e, Toast.LENGTH_SHORT).show() }
                    })
                    service.startRecording(file)
                }
                override fun onServiceDisconnected(n: android.content.ComponentName?) {}
            }, Context.BIND_AUTO_CREATE)
        }

        stopBtn.setOnClickListener { context.stopService(Intent(context, RecordingService::class.java)) }
        saveBtn.setOnClickListener { 
            recordedFile?.let { audioTools.addAudioFile(context, it, videoPreview.videoPlayer.currentPosition) }
            saveCurrentState(); dialog.dismiss() 
        }
        dialogView.findViewById<Button>(R.id.recordingCancelBtn).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ==========================================
    // أدوات الذكاء الاصطناعي والتصدير
    // ==========================================

    private fun showDubbingSelection() {
        AlertDialog.Builder(context).setTitle("اختر محرك الدبلجة").setItems(arrayOf("دبلجة سحابية (Edge TTS)", "دبلجة محلية (Multi-TTS)")) { _, w ->
            if (w == 0) startCloudDubbing() else startLocalDubbing()
        }.show()
    }

    private fun startCloudDubbing() {
        val subs = editorStateStore.getState().subtitles
        if (subs.isEmpty()) { Toast.makeText(context, "لا توجد ترجمة للدبلجة", Toast.LENGTH_SHORT).show(); return }
        progressOverlay.visibility = View.VISIBLE
        edgeTts.generateDubbingFiles(subs, context.getExternalFilesDir(null) ?: context.cacheDir, "ar", { _, _ -> }, { files ->
            (context as Activity).runOnUiThread {
                progressOverlay.visibility = View.GONE
                files.forEachIndexed { i, f -> audioTools.addAudioClip(f.absolutePath, 0L, subs[i].endMs - subs[i].startMs, subs[i].startMs) }
                saveCurrentState()
            }
        })
    }

    private fun startLocalDubbing() {
        val engines = localTts.getAvailableEngines()
        if (engines.isEmpty()) { Toast.makeText(context, "لا توجد محركات TTS", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(context).setTitle("اختر محرك").setItems(engines.map { it.label }.toTypedArray()) { _, i ->
            progressOverlay.visibility = View.VISIBLE
            localTts.init(engines[i].name) { ready ->
                if (ready) {
                    val subs = editorStateStore.getState().subtitles
                    localTts.generateDubbingFiles(subs, context.getExternalFilesDir(null) ?: context.cacheDir, "ar", { _, _ -> }, { files ->
                        (context as Activity).runOnUiThread {
                            progressOverlay.visibility = View.GONE
                            files.forEachIndexed { idx, f -> audioTools.addAudioClip(f.absolutePath, 0L, subs[idx].endMs - subs[idx].startMs, subs[idx].startMs) }
                            saveCurrentState()
                        }
                    })
                }
            }
        }.show()
    }

    private fun startExport() {
        val state = editorStateStore.getState()
        if (state.clips.isEmpty()) { Toast.makeText(context, "لا يوجد فيديو", Toast.LENGTH_SHORT).show(); return }
        val outFile = File(context.getExternalFilesDir(null), "final_export_${System.currentTimeMillis()}.mp4")
        progressOverlay.visibility = View.VISIBLE
        ExportEngine.exportProjectWithFFmpeg(context, Uri.EMPTY, outFile, state, 1.0f, 1.0f, 0.5f, { }, { s, m ->
            (context as Activity).runOnUiThread {
                progressOverlay.visibility = View.GONE
                if (!s) AlertDialog.Builder(context).setTitle("فشل").setMessage(m.take(300)).setPositiveButton("إغلاق", null).show()
                else Toast.makeText(context, "نجح التصدير!", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun quickMerge() { startExport() }

    // ==========================================
    // التحديث والمراقبة والدوال المفقودة
    // ==========================================

    private fun observeState() {
        editorStateStore.observe { state ->
            interactiveTimeline.setVideoAndAudioData(state.clips.sumOf { it.endMs - it.startMs }, state.clips, state.audioClips)
            interactiveTimeline.setSubtitleAndImageData(state.subtitles, state.imageClips)
            videoPreview.videoPlayer.applyVideoFilter(state.activeFilter)
            
            state.watermark?.let { wm ->
                watermarkOverlay.visibility = View.VISIBLE; watermarkOverlay.setImageURI(Uri.parse(wm.uri)); watermarkOverlay.alpha = wm.opacity
                watermarkOverlay.post { 
                    val p = watermarkOverlay.parent as View
                    watermarkOverlay.x = wm.xPercent * (p.width - watermarkOverlay.width)
                    watermarkOverlay.y = wm.yPercent * (p.height - watermarkOverlay.height)
                }
            } ?: run { watermarkOverlay.visibility = View.GONE }
        }
    }

    private fun updateUI() {
        val pos = videoPreview.videoPlayer.currentPosition
        interactiveTimeline.updatePlaybackTime(pos)
        timeText.text = String.format("%02d:%02d", pos / 60000, (pos / 1000) % 60)
        
        val state = editorStateStore.getState()
        val activeImg = state.imageClips.find { pos in it.timelineStartMs..(it.timelineStartMs + it.durationMs) }
        imageOverlay.visibility = if (activeImg != null) View.VISIBLE else View.GONE
        if (activeImg != null) imageOverlay.setImageURI(Uri.parse(activeImg.uri))
        
        val activeSub = state.subtitles.find { pos in it.startMs..it.endMs }
        subtitleOverlay.visibility = if (activeSub != null) View.VISIBLE else View.GONE
        if (activeSub != null) subtitleOverlay.text = activeSub.text
    }

    private fun saveCurrentState() {
        val state = editorStateStore.getState()
        repository.saveProject(repository.getProjectById(projectId)!!.copy(clips=state.clips, audioClips=state.audioClips, subtitles=state.subtitles, imageClips=state.imageClips, activeFilter=state.activeFilter, watermark=state.watermark, updatedAt=System.currentTimeMillis()))
    }

    fun refreshVideoPlayer() { videoPreview.videoPlayer.setVideoClips(editorStateStore.getState().clips) }

    fun loadWatermark(uri: Uri) { 
        tools.updateWatermark(WatermarkItem(uri = uri.toString()))
        saveCurrentState(); showWatermarkSettings(editorStateStore.getState().watermark!!)
    }
    
    fun loadImage(uri: Uri) { 
        val state = editorStateStore.getState()
        val newImg = ImageClip(UUID.randomUUID().toString(), uri.toString(), videoPreview.videoPlayer.currentPosition)
        editorStateStore.updateState(state.copy(imageClips = state.imageClips + newImg))
        saveCurrentState() 
    }
    
    fun loadSrtFile(uri: Uri) { 
        context.contentResolver.openInputStream(uri)?.use { 
            val subs = SrtParser.parse(it)
            val state = editorStateStore.getState()
            editorStateStore.updateState(state.copy(subtitles = state.subtitles + subs))
            saveCurrentState() 
        } 
    }
    
    fun setMultipleVideoUris(uris: List<Uri>) { 
        val retriever = android.media.MediaMetadataRetriever()
        val newClips = uris.map { 
            retriever.setDataSource(context, it)
            VideoClip(UUID.randomUUID().toString(), it.toString(), 0L, retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L) 
        }
        retriever.release()
        
        val state = editorStateStore.getState()
        val mutableClips = state.clips.toMutableList()
        
        if (pendingVideoInsertIndex != -1 && pendingVideoInsertIndex <= mutableClips.size) {
            mutableClips.addAll(pendingVideoInsertIndex, newClips)
            pendingVideoInsertIndex = -1
        } else {
            mutableClips.addAll(newClips)
        }
        
        editorStateStore.updateState(state.copy(clips = mutableClips))
        saveCurrentState()
        refreshVideoPlayer() 
    }
    
    fun releasePlayer() { videoPreview.release(); localTts.stop() }
}

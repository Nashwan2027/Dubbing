package dev.nash.dubbing.editor.export

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import dev.nash.dubbing.data.model.SubtitleItem
import dev.nash.dubbing.editor.state.EditorState
import java.io.File
import java.io.FileWriter
import java.util.Locale

object ExportEngine {

    fun exportProjectWithFFmpeg(
        context: Context,
        dummyUri: Uri,
        outputFile: File,
        state: EditorState,
        videoVol: Float,
        dubVol: Float,
        bgmVol: Float,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (state.clips.isEmpty()) { 
            onComplete(false, "لا توجد مقاطع فيديو للتصدير.")
            return 
        }

        val cmd = mutableListOf<String>()
        cmd.add("-y") // الكتابة فوق الملف القديم إذا وجد

        // 1. استيراد جميع مقاطع الفيديو
        state.clips.forEach { clip ->
            val safPath = FFmpegKitConfig.getSafParameterForRead(context, Uri.parse(clip.uri)) ?: clip.uri
            cmd.add("-i"); cmd.add(safPath)
        }

        // 2. استيراد جميع مقاطع الصوت الإضافية
        state.audioClips.forEach { audio ->
            cmd.add("-i"); cmd.add(audio.uri)
        }

        // 3. استيراد الصور والعلامة المائية
        state.imageClips.forEach { img -> cmd.add("-i"); cmd.add(img.uri) }
        val hasWatermark = state.watermark != null
        if (hasWatermark) { cmd.add("-i"); cmd.add(state.watermark!!.uri) }

        val filter = StringBuilder()
        
        // ==========================================
        // فلتر دمج الفيديو القوي (يمنع الانهيار)
        // ==========================================
        // نوحد كل مقاطع الفيديو إلى 1280x720 بـ 30 إطار، ونضيف صوت فارغ (anullsrc) إذا كان الفيديو صامتاً
        for (i in state.clips.indices) {
            filter.append("[$i:v]scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2,setsar=1,fps=30[v$i];")
            // استخدام amerge لضمان وجود مسار صوتي ستيريو (Stereo) قياسي لكل مقطع
            filter.append("aevalsrc=0:d=1[silence$i];[$i:a][silence$i]amix=inputs=2[a$i];")
        }

        // دمج الفيديوهات (Concat)
        for (i in state.clips.indices) filter.append("[v$i][a$i]")
        filter.append("concat=n=${state.clips.size}:v=1:a=1[v_concat][a_orig];")

        // ==========================================
        // دمج ومزج الصوتيات
        // ==========================================
        filter.append("[a_orig]volume=$videoVol[a_v_vol];")
        val audioInputs = mutableListOf("[a_v_vol]")
        val audioStartIndex = state.clips.size
        
        state.audioClips.forEachIndexed { i, clip ->
            val idx = audioStartIndex + i
            val delay = clip.timelineStartMs
            val vol = clip.volume * if (clip.type == "bgm") bgmVol else dubVol
            filter.append("[$idx:a]volume=$vol,adelay=$delay|$delay[aud$i];")
            audioInputs.add("[aud$i]")
        }
        
        if (audioInputs.size > 1) {
            filter.append("${audioInputs.joinToString("")}amix=inputs=${audioInputs.size}:duration=first[a_out];")
        } else {
            filter.append("[a_v_vol]anull[a_out];")
        }

        // ==========================================
        // تطبيق التراكبات البصرية (صور، فلاتر، ترجمة)
        // ==========================================
        var lastV = "[v_concat]"
        
        // الصور (Image Clips)
        val imageStartIndex = audioStartIndex + state.audioClips.size
        state.imageClips.forEachIndexed { i, img ->
            val idx = imageStartIndex + i
            val startT = img.timelineStartMs / 1000.0
            val endT = startT + (img.durationMs / 1000.0)
            filter.append("[$idx:v]scale=iw*${img.scale}:ih*${img.scale},format=rgba,colorchannelmixer=aa=${img.opacity}[img$i];")
            filter.append("$lastV[img$i]overlay=${img.xPercent*100}%:${img.yPercent*100}%:enable='between(t,$startT,$endT)'[v_img$i];")
            lastV = "[v_img$i]"
        }

        // العلامة المائية
        if (hasWatermark) {
            val wmIdx = imageStartIndex + state.imageClips.size
            val wm = state.watermark!!
            filter.append("[$wmIdx:v]scale=iw*${wm.scale}:ih*${wm.scale},format=rgba,colorchannelmixer=aa=${wm.opacity}[wm_s];")
            filter.append("$lastV[wm_s]overlay=${wm.xPercent*100}%:${wm.yPercent*100}%[v_wm];")
            lastV = "[v_wm]"
        }

        // فلتر الألوان
        if (state.activeFilter != "none") {
            val fType = when (state.activeFilter) {
                "grayscale" -> "hue=s=0"
                "inverted" -> "negate"
                "brightness" -> "eq=brightness=0.2"
                else -> "copy"
            }
            filter.append("$lastV $fType [v_filt];")
            lastV = "[v_filt]"
        }

        // الترجمة المنسقة
        if (state.subtitles.isNotEmpty()) {
            val srt = File(context.cacheDir, "export_${System.currentTimeMillis()}.srt")
            generateSrt(state.subtitles, srt)
            val srtPath = srt.absolutePath.replace(":", "\\:").replace("/", "/")
            val style = state.subtitleStyle
            val styleStr = "FontSize=${style.fontSize},PrimaryColour=${style.fontColor},OutlineColour=&H00000000,BorderStyle=${style.borderStyle},Alignment=${style.alignment}"
            filter.append("$lastV subtitles='$srtPath':force_style='$styleStr'[v_sub];")
            lastV = "[v_sub]"
        }

        // 6. تجميع الأمر النهائي
        cmd.add("-filter_complex"); cmd.add(filter.toString().trimEnd(';'))
        cmd.add("-map"); cmd.add(lastV)
        cmd.add("-map"); cmd.add("[a_out]")
        cmd.add("-c:v"); cmd.add("libx264")
        cmd.add("-preset"); cmd.add("fast")
        cmd.add("-crf"); cmd.add("26") // جودة جيدة وحجم ممتاز
        cmd.add("-c:a"); cmd.add("aac")
        cmd.add("-b:a"); cmd.add("128k")
        cmd.add(outputFile.absolutePath)

        val commandString = cmd.joinToString(" ")
        
        FFmpegKit.executeAsync(commandString) { session ->
            val returnCode = session.returnCode
            val logs = session.allLogsAsString
            if (ReturnCode.isSuccess(returnCode)) {
                onComplete(true, "تم تصدير الفيديو بنجاح.")
            } else {
                onComplete(false, logs)
            }
        }
    }

    private fun generateSrt(subs: List<SubtitleItem>, file: File) {
        FileWriter(file).use { w ->
            subs.forEachIndexed { i, s ->
                w.write("${i+1}\n${formatTime(s.startMs)} --> ${formatTime(s.endMs)}\n${s.text}\n\n")
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val s = (ms / 1000) % 60
        val m = (ms / 60000) % 60
        val h = (ms / 3600000)
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", h, m, s)
    }
}

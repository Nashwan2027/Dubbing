package dev.nash.dubbing.ai

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dev.nash.dubbing.data.model.SubtitleItem
import java.io.File
import java.util.Locale

class SystemTtsEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    fun init(engineName: String? = null, onReady: (Boolean) -> Unit) {
        tts = TextToSpeech(context, { status ->
            isReady = (status == TextToSpeech.SUCCESS)
            onReady(isReady)
        }, engineName)
    }

    fun getAvailableEngines(): List<TextToSpeech.EngineInfo> {
        return tts?.engines ?: emptyList()
    }

    fun generateDubbingFiles(
        subtitles: List<SubtitleItem>,
        outputDir: File,
        language: String = "ar",
        onProgress: (Int, Int) -> Unit,
        onComplete: (List<File>) -> Unit
    ) {
        if (!isReady) { onComplete(emptyList()); return }

        tts?.language = Locale(language)
        val files = mutableListOf<File>()
        var count = 0

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                count++
                onProgress(count, subtitles.size)
                if (count == subtitles.size) onComplete(files)
            }
            override fun onError(id: String?) { onComplete(files) }
        })

        subtitles.forEach { sub ->
            val file = File(outputDir, "local_tts_${sub.id}.wav")
            files.add(file)
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, sub.id)
            tts?.synthesizeToFile(sub.text, params, file, sub.id)
        }
    }

    fun stop() {
        tts?.stop()
        tts?.shutdown()
    }
}

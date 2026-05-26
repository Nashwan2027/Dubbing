package dev.nash.dubbing.ai

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import dev.nash.dubbing.data.model.SubtitleItem
import java.io.File
import java.util.Locale

class AiDubbingEngine(private val context: Context, private val onInit: (Boolean) -> Unit) : TextToSpeech.OnInitListener {
    
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var availableVoices: List<Voice> = emptyList()
    private var currentLangCode: String = "ar"

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            onInit(true)
        } else {
            isReady = false
            onInit(false)
        }
    }

    // دالة جديدة لتحديث اللغة قبل بدء التوليد
    private fun setupLanguage(langCode: String) {
        if (!isReady || tts == null) return
        
        currentLangCode = langCode
        val locale = Locale(langCode)
        val result = tts?.setLanguage(locale)
        
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // الرجوع للإنجليزية كبديل آمن
            tts?.setLanguage(Locale.US)
            currentLangCode = "en"
        }
        
        try {
            availableVoices = tts?.voices?.filter { it.locale.language == currentLangCode } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateDubbingFiles(
        subtitles: List<SubtitleItem>,
        outputDir: File,
        targetLangCode: String,
        onProgress: (Int, Int) -> Unit,
        onComplete: (List<File>) -> Unit
    ) {
        if (!isReady || tts == null) {
            onComplete(emptyList())
            return
        }

        setupLanguage(targetLangCode)

        val generatedFiles = mutableListOf<File>()
        var currentIndex = 0

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                currentIndex++
                onProgress(currentIndex, subtitles.size)
                
                if (currentIndex < subtitles.size) {
                    synthesizeNext(subtitles[currentIndex], outputDir, generatedFiles)
                } else {
                    onComplete(generatedFiles)
                }
            }
            override fun onError(utteranceId: String?) {
                onComplete(generatedFiles) 
            }
        })

        if (subtitles.isNotEmpty()) {
            synthesizeNext(subtitles[0], outputDir, generatedFiles)
        } else {
            onComplete(emptyList())
        }
    }

    private fun synthesizeNext(sub: SubtitleItem, outputDir: File, list: MutableList<File>) {
        val file = File(outputDir, "dub_auto_${sub.id}.wav")
        list.add(file)
        
        setVoiceByGender(sub.gender)

        val params = Bundle()
        tts?.synthesizeToFile(sub.text, params, file, sub.id)
    }

    private fun setVoiceByGender(gender: String) {
        if (availableVoices.isEmpty()) return

        val selectedVoice = availableVoices.find { voice ->
            val name = voice.name.lowercase()
            when (gender) {
                "female" -> name.contains("female") || name.contains("f")
                "male" -> name.contains("male") || name.contains("m")
                else -> false
            }
        } ?: availableVoices.first()

        try {
            tts?.voice = selectedVoice
            when (gender) {
                "female" -> tts?.setPitch(1.3f)
                "child" -> tts?.setPitch(1.8f)
                "male" -> tts?.setPitch(0.8f)
                else -> tts?.setPitch(1.0f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

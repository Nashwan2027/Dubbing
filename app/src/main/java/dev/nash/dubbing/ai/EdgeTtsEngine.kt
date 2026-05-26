package dev.nash.dubbing.ai

import android.content.Context
import dev.nash.dubbing.data.model.SubtitleItem
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class EdgeTtsEngine {

    private val voiceMap = mapOf(
        "ar" to mapOf("male" to "ar-SA-HamedNeural", "female" to "ar-SA-ZariyahNeural", "child" to "ar-SA-ZariyahNeural"),
        "en" to mapOf("male" to "en-US-GuyNeural", "female" to "en-US-JennyNeural", "child" to "en-US-AnaNeural"),
        "fr" to mapOf("male" to "fr-FR-HenriNeural", "female" to "fr-FR-DeniseNeural", "child" to "fr-FR-EloiseNeural"),
        "es" to mapOf("male" to "es-ES-AlvaroNeural", "female" to "es-ES-ElviraNeural", "child" to "es-ES-ElviraNeural"),
        "de" to mapOf("male" to "de-DE-ConradNeural", "female" to "de-DE-KatjaNeural", "child" to "de-DE-KatjaNeural")
    )

    fun generateDubbingFiles(
        subtitles: List<SubtitleItem>,
        outputDir: File,
        targetLangCode: String,
        onProgress: (Int, Int) -> Unit,
        onComplete: (List<File>) -> Unit
    ) {
        thread {
            val generatedFiles = mutableListOf<File>()
            val total = subtitles.size

            for (i in 0 until total) {
                val sub = subtitles[i]
                val voiceId = getVoiceId(targetLangCode, sub.gender)
                onProgress(i + 1, total)

                val file = File(outputDir, "dub_cloud_${sub.id}.mp3")
                val success = downloadTtsAudio(sub.text, voiceId, file)
                if (success) {
                    generatedFiles.add(file)
                }
            }
            onComplete(generatedFiles)
        }
    }

    private fun getVoiceId(langCode: String, gender: String): String {
        val langMap = voiceMap[langCode] ?: voiceMap["en"]!!
        return langMap[gender] ?: langMap["male"]!!
    }

    private fun downloadTtsAudio(text: String, voice: String, outputFile: File): Boolean {
        return try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val googleTtsUrl = "https://translate.google.com/translate_tts?ie=UTF-8&tl=${voice.substring(0, 2)}&client=tw-ob&q=$encodedText"

            val url = URL(googleTtsUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

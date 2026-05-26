package dev.nash.dubbing.editor.tools

import dev.nash.dubbing.data.model.SubtitleItem
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import java.util.regex.Pattern

object SrtParser {

    private val TIME_PATTERN = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})")

    fun parse(inputStream: InputStream): List<SubtitleItem> {
        val subtitles = mutableListOf<SubtitleItem>()
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        
        var line: String?
        var state = 0 // 0: ID, 1: Time, 2: Text
        var currentStartMs = 0L
        var currentEndMs = 0L
        val textBuilder = StringBuilder()

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line?.trim() ?: ""
            if (trimmed.isEmpty()) {
                if (textBuilder.isNotEmpty()) {
                    subtitles.add(
                        SubtitleItem(
                            id = UUID.randomUUID().toString(),
                            text = textBuilder.toString().trim(),
                            startMs = currentStartMs,
                            endMs = currentEndMs
                        )
                    )
                    textBuilder.setLength(0)
                }
                state = 0
                continue
            }

            when (state) {
                0 -> {
                    // تخطي سطر التعريف الرقمي والانتقال للتوقيت
                    state = 1
                }
                1 -> {
                    val times = trimmed.split("-->")
                    if (times.size == 2) {
                        currentStartMs = parseTimeToMs(times[0].trim())
                        currentEndMs = parseTimeToMs(times[1].trim())
                        state = 2
                    }
                }
                2 -> {
                    if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                    textBuilder.append(trimmed)
                }
            }
        }

        // حفظ الشريحة الأخيرة إن وجدت
        if (textBuilder.isNotEmpty()) {
            subtitles.add(
                SubtitleItem(
                    id = UUID.randomUUID().toString(),
                    text = textBuilder.toString().trim(),
                    startMs = currentStartMs,
                    endMs = currentEndMs
                )
            )
        }

        reader.close()
        return subtitles
    }

    private fun parseTimeToMs(timeStr: String): Long {
        val matcher = TIME_PATTERN.matcher(timeStr)
        if (matcher.find()) {
            val hrs = matcher.group(1)?.toLong() ?: 0L
            val mins = matcher.group(2)?.toLong() ?: 0L
            val secs = matcher.group(3)?.toLong() ?: 0L
            val ms = matcher.group(4)?.toLong() ?: 0L
            return (hrs * 3600000L) + (mins * 60000L) + (secs * 1000L) + ms
        }
        return 0L
    }
}

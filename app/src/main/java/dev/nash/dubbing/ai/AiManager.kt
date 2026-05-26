package dev.nash.dubbing.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import dev.nash.dubbing.BuildConfig
import dev.nash.dubbing.data.model.SubtitleItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException
import java.util.UUID
import kotlin.concurrent.thread

object AiManager {
    private const val PREFS_NAME = "ai_settings_prefs"
    private const val KEY_API_KEY = "user_configured_api_key"
    private const val TAG = "AiManager"

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = prefs.getString(KEY_API_KEY, null)
        return if (!key.isNullOrBlank()) key else BuildConfig.GEMINI_API_KEY
    }

    fun saveApiKey(context: Context, key: String) { 
        if (key.isNotBlank()) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_API_KEY, key.trim())
                .apply() 
        }
    }
    
    fun hasValidApiKey(context: Context): Boolean {
        val key = getApiKey(context)
        return key.isNotBlank() && key.startsWith("AIza")
    }

    fun extractAudioAndTranscribe(
        context: Context,
        mediaUri: Uri,
        targetLanguageName: String,
        onProgress: (String) -> Unit,
        onResult: (success: Boolean, subtitles: List<SubtitleItem>, errorMsg: String) -> Unit
    ) {
        // التحقق من صحة المفتاح أولاً
        if (!hasValidApiKey(context)) {
            onResult(false, emptyList(), "مفتاح API غير صالح أو مفقود. الرجاء إدخال مفتاح صحيح في الإعدادات.")
            return
        }
        
        val safInput = FFmpegKitConfig.getSafParameterForRead(context, mediaUri)
        if (safInput == null) {
            onResult(false, emptyList(), "لا يمكن قراءة ملف الوسائط.")
            return
        }

        val tempAudioFile = File(context.cacheDir, "extracted_stt_audio_${System.currentTimeMillis()}.mp3")
        tempAudioFile.deleteOnExit()

        onProgress("جاري استخراج وضغط الصوت من الفيديو...")

        val extractCmd = "-y -i $safInput -vn -c:a libmp3lame -b:a 32k -ac 1 ${tempAudioFile.absolutePath}"
        
        FFmpegKit.executeAsync(extractCmd) { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                onProgress("تم الاستخراج. جاري التحليل بواسطة الذكاء الاصطناعي...")
                sendToGemini(context, tempAudioFile, targetLanguageName, onResult)
            } else {
                val error = session.failStackTrace?.take(200) ?: "فشل استخراج الصوت"
                onResult(false, emptyList(), "فشل في استخراج الصوت: $error")
            }
        }
    }

    private fun sendToGemini(
        context: Context,
        audioFile: File,
        targetLanguageName: String,
        onResult: (Boolean, List<SubtitleItem>, String) -> Unit
    ) {
        val apiKey = getApiKey(context) 
        if (apiKey.isBlank()) { 
            onResult(false, emptyList(), "مفتاح API مفقود.")
            return 
        }

        thread {
            var connection: HttpURLConnection? = null
            try {
                val base64 = Base64.encodeToString(audioFile.readBytes(), Base64.NO_WRAP)
                val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
                val url = URL(urlStr)
                connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    connectTimeout = 30000
                    readTimeout = 120000
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                }

                val prompt = """
                    Listen to this audio carefully. Perform Speaker Diarization.
                    Transcribe and translate the dialogue to $targetLanguageName.
                    You MUST return ONLY a valid JSON array of objects. Do not use markdown blocks or any other text.
                    For each dialogue segment, provide:
                    "text": the translated text,
                    "startMs": start time in milliseconds (integer),
                    "endMs": end time in milliseconds (integer),
                    "speaker": a unique label (e.g., "Speaker_1", "Speaker_2"),
                    "gender": predict the voice gender ("male", "female", or "child")
                    
                    Example response format:
                    [{"text":"Hello","startMs":0,"endMs":2000,"speaker":"Speaker_1","gender":"male"}]
                """.trimIndent()

                val jsonReq = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply { 
                                    put("mimeType", "audio/mp3")
                                    put("data", base64) 
                                })
                            })
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    }))
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.4)
                        put("topP", 0.8)
                    })
                }

                OutputStreamWriter(connection.outputStream).use { it.write(jsonReq.toString()) }
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        val resp = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) resp.append(line)
                        
                        val resJson = JSONObject(resp.toString())
                        val candidates = resJson.getJSONArray("candidates")
                        if (candidates.length() == 0) {
                            onResult(false, emptyList(), "لم يتم الحصول على رد من الذكاء الاصطناعي")
                            return@thread
                        }
                        
                        var textResponse = candidates
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            
                        textResponse = textResponse
                            .replace("```json", "")
                            .replace("```", "")
                            .trim()

                        val jsonArray = JSONArray(textResponse)
                        val subsList = mutableListOf<SubtitleItem>()
                        
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            subsList.add(
                                SubtitleItem(
                                    id = UUID.randomUUID().toString(),
                                    text = item.getString("text"),
                                    startMs = item.getLong("startMs"),
                                    endMs = item.getLong("endMs"),
                                    speaker = item.optString("speaker", "Speaker_1"),
                                    gender = item.optString("gender", "male").lowercase()
                                )
                            )
                        }
                        
                        if (subsList.isEmpty()) {
                            onResult(false, emptyList(), "لم يتم العثور على نصوص في الرد")
                        } else {
                            onResult(true, subsList, "")
                        }
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorMsg = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { reader ->
                            reader.readText()
                        }
                    } else "HTTP Error: $responseCode"
                    
                    Log.e(TAG, "API Error: $errorMsg")
                    onResult(false, emptyList(), "خطأ في الخادم: ${errorMsg.take(100)}")
                }
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Timeout", e)
                onResult(false, emptyList(), "انتهى الوقت المحدد. يرجى المحاولة مرة أخرى.")
            } catch (e: Exception) { 
                Log.e(TAG, "Exception", e)
                onResult(false, emptyList(), e.localizedMessage ?: "خطأ غير معروف") 
            } finally {
                connection?.disconnect()
                audioFile.delete()
            }
        }
    }
}

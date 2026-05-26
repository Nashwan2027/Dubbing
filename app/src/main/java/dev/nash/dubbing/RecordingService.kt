package dev.nash.dubbing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    private val binder = LocalBinder()
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: File? = null
    private var recordingStartTime: Long = 0L
    private var listener: RecordingListener? = null

    companion object {
        private const val CHANNEL_ID = "recording_service_channel"
        private const val NOTIFICATION_ID = 101
        
        fun getRecordingFile(context: Context): File {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            return File(context.getExternalFilesDir(null), "recording_$timestamp.mp4")
        }
    }
    
    interface RecordingListener {
        fun onRecordingStarted(file: File)
        fun onRecordingStopped(file: File, durationMs: Long)
        fun onRecordingError(error: String)
    }

    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
        return START_NOT_STICKY
    }

    fun setRecordingListener(listener: RecordingListener?) {
        this.listener = listener
    }

    fun startRecording(file: File = getRecordingFile(this)): Boolean {
        if (isRecording) {
            listener?.onRecordingError("جاري التسجيل بالفعل")
            return false
        }
        
        outputFile = file
        
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(applicationContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                
                try {
                    prepare()
                    start()
                    isRecording = true
                    recordingStartTime = System.currentTimeMillis()
                    listener?.onRecordingStarted(file)
                    
                    // تحديث الإشعار
                    updateNotification("جاري التسجيل... ${file.name}")
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                    listener?.onRecordingError("فشل بدء التسجيل: ${e.message}")
                    return false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listener?.onRecordingError("خطأ في تهيئة المسجل: ${e.message}")
            return false
        }
    }

    fun stopRecording(): String? {
        if (!isRecording) return null
        
        val duration = System.currentTimeMillis() - recordingStartTime
        
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
        }
        
        outputFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                listener?.onRecordingStopped(file, duration)
                updateNotification("اكتمل التسجيل", false)
            } else {
                listener?.onRecordingError("فشل التسجيل: الملف فارغ أو تالف")
            }
        }
        
        @Suppress("DEPRECATION")
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        return outputFile?.absolutePath
    }

    fun isRecordingActive(): Boolean = isRecording

    fun getOutputFile(): File? = outputFile
    
    fun getRecordingDuration(): Long = if (isRecording) System.currentTimeMillis() - recordingStartTime else 0L

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("تسجيل الدبلجة")
            .setContentText("الميكروفون نشط...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(message: String, isOngoing: Boolean = true) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("تسجيل الدبلجة")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(isOngoing)
            .build()
        
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "تسجيل الصوت",
                NotificationManager.IMPORTANCE_LOW
            )
            serviceChannel.description = "قناة تسجيل الصوت لاستوديو الدبلجة"
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        if (isRecording) {
            stopRecording()
        }
        super.onDestroy()
    }
}

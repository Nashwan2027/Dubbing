package dev.nash.dubbing.media.preview

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ThumbnailLoader(private val context: Context, private val onUpdate: () -> Unit) {

    private val memoryCache: LruCache<String, Bitmap>
    private val executor: ExecutorService = Executors.newFixedThreadPool(4)
    private var isShutdown = false

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    fun getThumbnail(uriString: String, timeMs: Long, targetWidth: Int, targetHeight: Int): Bitmap? {
        if (isShutdown) return null
        
        val key = "${uriString}_${timeMs / 1000}"
        
        val cached = memoryCache.get(key)
        if (cached != null && !cached.isRecycled) return cached

        executor.execute {
            if (isShutdown) return@execute
            if (memoryCache.get(key) == null) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, Uri.parse(uriString))
                    
                    var bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    retriever.release()

                    if (bitmap != null && !bitmap.isRecycled) {
                        val scaledBitmap = scaleCenterCrop(bitmap, targetWidth, targetHeight)
                        if (!bitmap.isRecycled && bitmap != scaledBitmap) {
                            bitmap.recycle()
                        }
                        if (!isShutdown && scaledBitmap != null && !scaledBitmap.isRecycled) {
                            memoryCache.put(key, scaledBitmap)
                            onUpdate()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    private fun scaleCenterCrop(source: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        if (source.isRecycled) return source
        
        val sourceWidth = source.width
        val sourceHeight = source.height

        val scale = maxOf(newWidth.toFloat() / sourceWidth, newHeight.toFloat() / sourceHeight)
        
        val scaledWidth = (scale * sourceWidth).toInt()
        val scaledHeight = (scale * sourceHeight).toInt()

        val left = (newWidth - scaledWidth) / 2
        val top = (newHeight - scaledHeight) / 2

        val targetBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(targetBitmap)
        
        val matrix = android.graphics.Matrix()
        matrix.postScale(scale, scale)
        matrix.postTranslate(left.toFloat(), top.toFloat())
        
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        
        return targetBitmap
    }

    fun clearCache() {
        memoryCache.evictAll()
    }

    fun shutdown() {
        isShutdown = true
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
        clearCache()
    }
}

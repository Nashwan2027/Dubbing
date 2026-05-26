package dev.nash.dubbing.media.chooser

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

class MediaChooser(
    private val launcher: ActivityResultLauncher<String>
) {
    private var onSelected: ((Uri?) -> Unit)? = null

    fun setOnMediaSelectedListener(listener: (Uri?) -> Unit) {
        onSelected = listener
    }

    fun openVideoPicker() {
        launcher.launch("video/*")
    }

    fun dispatchResult(uri: Uri?) {
        onSelected?.invoke(uri)
    }
}

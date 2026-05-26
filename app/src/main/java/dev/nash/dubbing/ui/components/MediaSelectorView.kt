package dev.nash.dubbing.ui.components

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout

class MediaSelectorView(context: Context) : LinearLayout(context) {
    private var listener: ((Uri?) -> Unit)? = null

    init {
        orientation = VERTICAL
    }

    fun bindActivity(activity: AppCompatActivity) {
    }

    fun setOnMediaSelectedListener(onSelected: (Uri?) -> Unit) {
        listener = onSelected
    }

    fun dispatchSelected(uri: Uri?) {
        listener?.invoke(uri)
    }
}

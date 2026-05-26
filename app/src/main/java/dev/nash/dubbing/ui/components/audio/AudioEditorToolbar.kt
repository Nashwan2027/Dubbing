package dev.nash.dubbing.ui.components.audio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Button
import dev.nash.dubbing.R

class AudioEditorToolbar(context: Context) : LinearLayout(context) {

    private val recordButton: Button
    private val mixerButton: Button
    private val fadeButton: Button
    private val volumeButton: Button

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.audio_editor_toolbar, this, true)

        recordButton = findViewById(R.id.buttonRecord)
        mixerButton = findViewById(R.id.buttonMixer)
        fadeButton = findViewById(R.id.buttonFade)
        volumeButton = findViewById(R.id.buttonVolume)
    }

    fun setOnRecordClickListener(listener: View.OnClickListener) {
        recordButton.setOnClickListener(listener)
    }

    fun setOnMixerClickListener(listener: View.OnClickListener) {
        mixerButton.setOnClickListener(listener)
    }

    fun setOnFadeClickListener(listener: View.OnClickListener) {
        fadeButton.setOnClickListener(listener)
    }

    fun setOnVolumeClickListener(listener: View.OnClickListener) {
        volumeButton.setOnClickListener(listener)
    }
}

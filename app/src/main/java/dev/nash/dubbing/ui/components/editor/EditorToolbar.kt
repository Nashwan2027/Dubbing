package dev.nash.dubbing.ui.components.editor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Button
import dev.nash.dubbing.R

class EditorToolbar(context: Context) : LinearLayout(context) {

    private val cutButton: Button
    private val trimButton: Button
    private val speedButton: Button
    private val filterButton: Button
    private val overlayButton: Button

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.editor_toolbar, this, true)

        cutButton = findViewById(R.id.buttonCut)
        trimButton = findViewById(R.id.buttonTrim)
        speedButton = findViewById(R.id.buttonSpeed)
        filterButton = findViewById(R.id.buttonFilter)
        overlayButton = findViewById(R.id.buttonOverlay)
    }

    fun setOnCutClickListener(listener: View.OnClickListener) {
        cutButton.setOnClickListener(listener)
    }

    fun setOnTrimClickListener(listener: View.OnClickListener) {
        trimButton.setOnClickListener(listener)
    }

    fun setOnSpeedClickListener(listener: View.OnClickListener) {
        speedButton.setOnClickListener(listener)
    }

    fun setOnFilterClickListener(listener: View.OnClickListener) {
        filterButton.setOnClickListener(listener)
    }

    fun setOnOverlayClickListener(listener: View.OnClickListener) {
        overlayButton.setOnClickListener(listener)
    }
}

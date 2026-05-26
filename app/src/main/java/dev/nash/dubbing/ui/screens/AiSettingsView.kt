package dev.nash.dubbing.ui.screens

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import dev.nash.dubbing.ai.AiManager

class AiSettingsView(context: Context) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setPadding(40, 40, 40, 40)
        setBackgroundColor(Color.parseColor("#121212"))

        val title = TextView(context).apply {
            text = "إعدادات الذكاء الاصطناعي 🤖"
            textSize = 24f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 40)
        }

        val desc = TextView(context).apply {
            text = "لاستخدام ميزة التفريغ الصوتي (STT) والدبلجة (Diarization)، الرجاء إدخال مفتاح Google Gemini API الخاص بك."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 40)
        }

        val apiKeyInput = EditText(context).apply {
            hint = "أدخل API Key هنا..."
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.DKGRAY)
            setText(AiManager.getApiKey(context)) // جلب المفتاح الحالي
        }

        val saveButton = Button(context).apply {
            text = "حفظ المفتاح"
            setBackgroundColor(Color.parseColor("#2E5BFF"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val key = apiKeyInput.text.toString().trim()
                if (key.isNotEmpty()) {
                    AiManager.saveApiKey(context, key)
                    Toast.makeText(context, "تم حفظ المفتاح بنجاح!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "لا يمكن ترك الحقل فارغاً", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val edgeDesc = TextView(context).apply {
            text = "\nملاحظة: الدبلجة الصوتية (TTS) ستعتمد على خدمة Edge TTS السحابية المجانية."
            textSize = 12f
            setTextColor(Color.parseColor("#FFD60A"))
        }

        addView(title)
        addView(desc)
        addView(apiKeyInput)
        addView(saveButton)
        addView(edgeDesc)
    }
}

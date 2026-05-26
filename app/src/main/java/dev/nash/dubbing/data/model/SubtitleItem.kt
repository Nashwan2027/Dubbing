package dev.nash.dubbing.data.model

data class SubtitleItem(
    val id: String,
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val speaker: String = "Speaker_1", // مثال: Speaker_A, Speaker_B
    val gender: String = "male",       // "male", "female", "child"
    val xPercent: Float = 0.35f,
    val yPercent: Float = 0.8f
)

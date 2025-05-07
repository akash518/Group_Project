package com.example.groupproject

import android.graphics.Color

object CourseColorManager {
    val colorPalette = listOf(
        "#E53935", // Red
        "#FB8C00", // Orange
        "#43A047", // Green
        "#1E88E5", // Blue
        "#8E24AA", // Purple
        "#00897B", // Teal
        "#FDD835", // Yellow
        "#795548", // Brown
        "#607D8B"  // Gray-blue
    )

    private val courseColors = mutableMapOf<String, Int>()

    fun setColorForCourse(courseId: String, hexColor: String) {
        try {
            val parsed = Color.parseColor(hexColor)
            courseColors[courseId] = parsed
        } catch (e: IllegalArgumentException) {
            courseColors[courseId] = Color.LTGRAY
        }
    }

    fun getColor(courseId: String): Int {
        return courseColors[courseId] ?: Color.LTGRAY
    }

    fun getAllColors(): Map<String, Int> = courseColors.toMap()

    fun reset() {
        courseColors.clear()
    }
}

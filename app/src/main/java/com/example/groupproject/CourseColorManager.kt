package com.example.groupproject

import android.graphics.Color

/**
 * Singleton object to manage colors assigned to each course.
 */
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

    // Stores the assigned color for each course using its ID
    private val courseColors = mutableMapOf<String, Int>()

    /**
     * Assigns a specific color to a course using its hex string.
     * If the hex color is invalid, it defaults to light gray.
     * @param courseId - The identifier of the course
     * @param hexColor - The hex string to assign
     */
    fun setColorForCourse(courseId: String, hexColor: String) {
        try {
            val parsed = Color.parseColor(hexColor) // Convert hex to Android color int
            courseColors[courseId] = parsed // Assign to course
        } catch (e: IllegalArgumentException) {
            courseColors[courseId] = Color.LTGRAY // Fallback to light gray if parsing fails
        }
    }

    /**
     * Returns a copy of all course-color assignments.
     * @return Map of course IDs to color int values
     */
    fun getAllColors(): Map<String, Int> = courseColors.toMap()

    /**
     * Clears all assigned course colors.
     */
    fun reset() {
        courseColors.clear()
    }
}

package com.example.groupproject

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.min

/**
 * Data class representing a course and its progress.
 * @param courseId Identifier of the course
 * @param progress Float between 0 and 1 representing completion
 */
data class CourseProgress(val courseId: String, val progress: Float)

/**
 * Custom view that displays circular progress rings for each course.
 * Each ring represents a course, its completion progress, and has an animated transition.
 */
class ProgressView(context: Context, attrs: AttributeSet? = null): View(context, attrs) {
    private var courses = listOf<CourseProgress>()
    private var ringWidth = 100f
    private var spacing = 15f

    private var courseColors: Map<String, Int> = emptyMap()

    private var currentProgress = listOf<CourseProgress>()
    private var selectedCourseId: String? = null

    /**
     * Updates the course progress list with animation.
     */
    fun updateProgress(newList: List<CourseProgress>) {
        Log.d("ProgressView", "updateProgress called with ${newList.size} courses")

        val startList = currentProgress
        val animatedList = mutableListOf<CourseProgress>()

        // Create animation from 0 to 1 to interpolate between current and new progress
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 800
        // Use a decelerate interpolator to start fast and end slowly
        animator.interpolator = DecelerateInterpolator()

        animator.addUpdateListener { animation ->
            // Get the current progress of the animation as a fraction between 0 and 1
            val fraction = animation.animatedValue as Float
            animatedList.clear()

            // Interpolate each course's progress
            for (i in newList.indices) {
                // Get the starting progress from the current state, or 0 if not present
                val start = startList.getOrNull(i)?.progress ?: 0f
                // Get the target progress from the new data
                val end = newList[i].progress
                // Interpolate the progress value based on the current animation fraction
                val interpolated = start + (end - start) * fraction
                // Add the new interpolated progress state to the animated list
                animatedList.add(CourseProgress(newList[i].courseId, interpolated))
            }

            this.courses = animatedList
            invalidate()
        }

        animator.start()
        currentProgress = newList
    }

    /**
     * Highlights only the ring of the selected course.
     */
    fun setSelectedCourse(courseId: String?) {
        selectedCourseId = courseId
        invalidate()
    }

    /**
     * Updates the mapping of course IDs to their associated ring colors.
     */
    fun setCourseColors(colors: Map<String, Int>) {
        courseColors = colors
        invalidate()
    }

    /**
     * Called when the view needs to be drawn.
     * Draws concentric animated rings for each course and its progress.
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val numberOfCourses = courses.size
        val denominator = 4f
        val availableRadius = (min(width, height) / denominator)
        val totalSpacing = (numberOfCourses - 1) * spacing
        val maxWidth = availableRadius - totalSpacing

        ringWidth = if(numberOfCourses == 1) (100f) else maxWidth / numberOfCourses
        var radius = if (numberOfCourses == 1) (availableRadius) else ringWidth * numberOfCourses + spacing * (numberOfCourses - 1)
        val isAllCourses = selectedCourseId == null
//        Log.d("ProgressView", "Radius: $radius")
        Log.d("ProgressView", "Available Radius: $availableRadius")
        Log.d("ProgressView", "Total Courses: $numberOfCourses")
//        Log.d("ProgressView", "Total Spacing: $totalSpacing")
//        Log.d("ProgressView", "Max Width: $maxWidth")
//        Log.d("ProgressView", "Ring Width: $ringWidth")

        // Draw each course ring
        for (i in courses.indices) {
            val course = courses[i]
            val isSelected = isAllCourses || course.courseId == selectedCourseId

            // Base ring (gray circle)
            val basePaint = Paint().apply {
                style = Paint.Style.STROKE
                color = Color.LTGRAY
                strokeWidth = ringWidth
                isAntiAlias = true
            }

            // Progress ring (colored arc)
            val progressPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                color = if (isSelected) {
                    courseColors[course.courseId] ?: Color.LTGRAY
                } else {
                    Color.LTGRAY
                }
                strokeWidth = ringWidth
                isAntiAlias = true
            }

            // Define the bounding box of the ring (circle)
            val rect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

            // Draw background circle
            canvas.drawArc(rect, 0f, 360f, false, basePaint)
            // Draw progress arc only if there's actual progress
            if (course.progress > 0f) {
                canvas.drawArc(rect, -90f, 360f * course.progress, false, progressPaint)
            }

            // Move outward for next ring
            radius += (ringWidth + spacing)
        }
    }
}
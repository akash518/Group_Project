package com.example.groupproject

import android.animation.ValueAnimator
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

data class CourseProgress(val courseId: String, val progress: Float)

class ProgressView(context: Context, attrs: AttributeSet? = null): View(context, attrs) {
    private var courses = listOf<CourseProgress>()
    private var ringWidth = 100f
    private var spacing = 15f

    private var courseColors: Map<String, Int> = emptyMap()

    private var currentProgress = listOf<CourseProgress>()
    private var selectedCourseId: String? = null

    fun updateProgress(newList: List<CourseProgress>) {
        Log.d("HomeActivity", "updateProgress called with ${newList.size} courses")

        val startList = currentProgress
        val animatedList = mutableListOf<CourseProgress>()

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 800
        animator.interpolator = DecelerateInterpolator()

        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            animatedList.clear()

            for (i in newList.indices) {
                val start = startList.getOrNull(i)?.progress ?: 0f
                val end = newList[i].progress
                val interpolated = start + (end - start) * fraction
                animatedList.add(CourseProgress(newList[i].courseId, interpolated))
            }

            this.courses = animatedList
            invalidate()
        }

        animator.start()
        currentProgress = newList
    }

    fun setSelectedCourse(courseId: String?) {
        selectedCourseId = courseId
        invalidate()
    }

    fun setCourseColors(colors: Map<String, Int>) {
        courseColors = colors
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
//        Log.w("HomeActivity", "onDraw")
//        Log.d("HomeActivity", "Courses: $courses")

        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
//        var radius = (min(width, height) / 4f) - ringWidth
        val availableRadius = (min(width, height) / 3f) - 60
        val numberOfCourses = courses.size
        val totalSpacing = (numberOfCourses - 1) * spacing
        val maxWidth = availableRadius - totalSpacing

        ringWidth = if(numberOfCourses == 1) (100f) else maxWidth / numberOfCourses
        var radius = if (numberOfCourses == 1) (availableRadius) else ringWidth * numberOfCourses + spacing * (numberOfCourses - 1)
        val isAllCourses = selectedCourseId == null
//        Log.d("ProgressView", "Radius: $radius")
//        Log.d("ProgressView", "Available Radius: $availableRadius")
//        Log.d("ProgressView", "Total Courses: $numberOfCourses")
//        Log.d("ProgressView", "Total Spacing: $totalSpacing")
//        Log.d("ProgressView", "Max Width: $maxWidth")
//        Log.d("ProgressView", "Ring Width: $ringWidth")

        for (i in courses.indices) {
            val course = courses[i]
            val isSelected = isAllCourses || course.courseId == selectedCourseId

            val basePaint = Paint().apply {
                style = Paint.Style.STROKE
                color = Color.LTGRAY
                strokeWidth = ringWidth
                isAntiAlias = true
            }

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

            val rect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

            canvas.drawArc(rect, 0f, 360f, false, basePaint)
            if (course.progress > 0f) {
                canvas.drawArc(rect, -90f, 360f * course.progress, false, progressPaint)
            }

            radius += (ringWidth + spacing)
        }
    }
}
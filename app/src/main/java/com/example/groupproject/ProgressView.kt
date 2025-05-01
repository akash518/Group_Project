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
    private val ringWidth = 40f
    private val spacing = 15f

    private val courseColors = listOf(
        Color.parseColor("#E53935"), // Red
        Color.parseColor("#FB8C00"), // Orange
        Color.parseColor("#43A047"), // Green
        Color.parseColor("#1E88E5"), // Blue
        Color.parseColor("#8E24AA"), // Purple
        Color.parseColor("#00897B"), // Teal
        Color.parseColor("#FDD835"), // Yellow
    )

    private var currentProgress = listOf<CourseProgress>()

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


    override fun onDraw(canvas: Canvas) {
//        Log.w("HomeActivity", "onDraw")
//        Log.d("HomeActivity", "Courses: $courses")

        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        var radius = (min(width, height) / 2f) - ringWidth

        for (i in courses.indices) {
            val course = courses[i]

            val paint = Paint().apply {
                style = Paint.Style.STROKE
                color = Color.LTGRAY
                strokeWidth = ringWidth
                isAntiAlias = true
            }

            val progressPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                color = courseColors[i % courseColors.size]
                strokeWidth = ringWidth
                isAntiAlias = true
            }

            val rect = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

            canvas.drawArc(rect, 0f, 360f, false, paint)
            canvas.drawArc(rect, -90f, 360f * course.progress, false, progressPaint)

            radius -= (ringWidth + spacing)
        }
    }
}
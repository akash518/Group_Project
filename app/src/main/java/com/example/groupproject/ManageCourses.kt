package com.example.groupproject

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs

/**
 * Activity for managing courses the user has created.
 * Displays a list of courses with delete buttons.
 * Swipe right to return to the previous screen.
 */
class ManageCourses : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CourseAdapter
    private val courses = mutableListOf<String>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Courses"

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val titleTextView = TextView(this).apply {
            text = context.getString(R.string.manage_courses)
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 50, 0, 30)
        }

        val gestureDetector = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener() {
            private val swipeThreshold = 100
            private val swipeVelocityThreshold = 100

            override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
                if (p0 == null) return false
                val diffX = p1.x - p0.x
                val diffY = p1.y - p0.y
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > swipeThreshold && abs(p2) > swipeVelocityThreshold) {
                        finish()
                        return true
                    }
                }
                return false
            }
        })

        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener {_, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        recyclerView = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            layoutManager = LinearLayoutManager(this@ManageCourses)
        }

        recyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        rootLayout.addView(titleTextView)
        rootLayout.addView(recyclerView)
        setContentView(rootLayout)

        loadCourses()
    }

    /**
     * Loads the user's course names from Firestore, sets colors, and populates the RecyclerView.
     */
    private fun loadCourses() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("courses")
            .get().addOnSuccessListener { result ->
                courses.clear()
                for (doc in result) {
                    val courseId = doc.id
                    val color = doc.getString("color") ?: "#9E9E9E"
                    CourseColorManager.setColorForCourse(courseId, color)
                    courses.add(courseId)
                }

                // Create adapter with delete handler
                adapter = CourseAdapter(courses, CourseColorManager.getAllColors()) { courseId ->
                    confirmDelete(courseId)
                }
                recyclerView.adapter = adapter
            }
    }

    /**
     * Shows a confirmation dialog before deleting a course.
     * @param courseId The ID of the course to delete.
     */
    private fun confirmDelete(courseId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete \"$courseId\"?")
            .setPositiveButton("Delete") { _, _ -> deleteCourse(courseId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Deletes a course document from Firestore and removes it from the view.
     * @param courseId The ID of the course to delete.
     */
    private fun deleteCourse(courseId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("courses").document(courseId)
            .delete()
            .addOnSuccessListener {
                val index = courses.indexOf(courseId)
                if (index != -1) {
                    courses.removeAt(index)
                    adapter.notifyItemRemoved(index)
                }
                Toast.makeText(this, "$courseId deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete $courseId", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

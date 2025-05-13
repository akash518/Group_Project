package com.example.groupproject

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
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
 * Displays all tasks from all courses in a scrollable list.
 * Supports swipe gestures to navigate back or go to ManageCourses.
 */
class FullTaskViewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val allTasks = mutableListOf<Task>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable "back" arrow and set title on the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "All Tasks"

        // Create vertical layout programmatically
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Title TextView at the top
        val titleTextView = TextView(this).apply {
            text = context.getString(R.string.all_tasks)
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 50, 0, 30)
        }

        // Gesture detector to handle swipes left and right
        val gestureDetector = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener() {
            private val swipeVelocityThreshold = 100

            override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
                if (p0 == null) return false
                val diffX = p1.x - p0.x
                val diffY = p1.y - p0.y
                if (abs(diffX) > abs(diffY)) {
                    if (diffX > 0 && abs(p2) > swipeVelocityThreshold) {
                        // Swipe right -> go back
                        finish()
                    } else {
                        // Swipe left -> go to ManageCourses screen
                        startActivity(Intent(this@FullTaskViewActivity, ManageCourses::class.java))
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            overrideActivityTransition(
                                Activity.OVERRIDE_TRANSITION_OPEN,
                                R.anim.slide_in_left,
                                R.anim.slide_out_right
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        }
                        return true
                    }
                }
                return false
            }
        })

        // Attach gesture detection to the full screen
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener {_, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        // Create and configure RecyclerView for showing tasks
        recyclerView = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            layoutManager = LinearLayoutManager(this@FullTaskViewActivity)
        }

        // Pass touch events on RecyclerView to gesture detector
        recyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        // Add views to the layout and display
        rootLayout.addView(titleTextView)
        rootLayout.addView(recyclerView)
        setContentView(rootLayout)

        // Load all tasks from Firebase
        loadAllTasks()
    }

    // Handles action bar back arrow click
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    /**
     * Loads all tasks from all courses belonging to the user.
     * Sorts them by status and due date, then displays them in the RecyclerView.
     */
    private fun loadAllTasks() {
        val userId = auth.currentUser?.uid ?: return
        allTasks.clear()

        // Get all user's courses
        db.collection("users").document(userId).collection("courses")
            .get().addOnSuccessListener { courses ->
                var loadedCourses = 0

                for (courseDoc in courses) {
                    val courseId = courseDoc.id

                    // For each course, get its tasks
                    db.collection("users").document(userId).collection("courses")
                        .document(courseId).collection("tasks").get()
                        .addOnSuccessListener { tasks ->
                            // Convert each document into a Task object
                            for (taskDoc in tasks) {
                                val taskName = taskDoc.id
                                val completed = taskDoc.getBoolean("completed") == true
                                val timestamp = taskDoc.getTimestamp("dueDate")
                                val dueDate = timestamp?.toDate()
                                val formatted = if (dueDate != null) {
                                    "Due " + android.text.format.DateFormat.format("MMM d 'at' h:mm a", dueDate)
                                } else {
                                    "No due date"
                                }

                                allTasks.add(Task(courseId, taskName, completed, formatted, dueDate))
                            }

                            loadedCourses++
                            if (loadedCourses == courses.size()) {
                                val now = java.util.Calendar.getInstance().time
                                val sortedTasks = allTasks.sortedWith(compareBy<Task> {
                                    when {
                                        !it.isCompleted && it.dueDate != null && it.dueDate.before(now) -> 0 // Overdue
                                        !it.isCompleted -> 1 // In Progress
                                        else -> 2 // Completed
                                    }
                                }.thenBy { it.dueDate }) // Optional within-group sorting

                                // Load each course's color from Firestore
                                val distinctCourses = allTasks.map { it.courseId }.distinct()
                                for (courseId in distinctCourses) {
                                    db.collection("users").document(userId).collection("courses")
                                        .document(courseId).get()
                                        .addOnSuccessListener { doc ->
                                            val hex = doc.getString("color") ?: "#9E9E9E"
                                            CourseColorManager.setColorForCourse(courseId, hex)
                                        }
                                }

                                // Set the RecyclerView adapter to show tasks
                                recyclerView.adapter = TaskAdapter(sortedTasks.toMutableList(), { task ->
                                    // Mark task complete
                                    val taskRef = db.collection("users").document(userId)
                                        .collection("courses").document(task.courseId)
                                        .collection("tasks").document(task.taskName)

                                    taskRef.update("completed", true).addOnSuccessListener {
                                        Toast.makeText(this, "Marked ${task.taskName} as complete", Toast.LENGTH_SHORT).show()
                                        allTasks.clear()
                                        loadAllTasks()
                                    }
                                }, CourseColorManager.getAllColors(), { task ->

                                    val taskRef = db.collection("users").document(userId)
                                        .collection("courses").document(task.courseId)
                                        .collection("tasks").document(task.taskName)

                                    // Delete task
                                    taskRef.delete().addOnSuccessListener {
                                        Toast.makeText(this, "Deleted ${task.taskName}", Toast.LENGTH_SHORT).show()
                                        // Remove from adapter without refreshing full list
                                        val adapter = recyclerView.adapter as? TaskAdapter
                                        val index = adapter?.tasks?.indexOfFirst {
                                            it.courseId == task.courseId && it.taskName == task.taskName
                                        } ?: -1

                                        if (index != -1) {
                                            adapter?.tasks?.removeAt(index)
                                            adapter?.notifyItemRemoved(index)
                                        }

                                    }.addOnFailureListener {
                                        Toast.makeText(this, "Failed to delete ${task.taskName}", Toast.LENGTH_SHORT).show()
                                    }
                                })
                            }
                        }
                }
            }
    }
}

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

class FullTaskViewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val allTasks = mutableListOf<Task>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "All Tasks"



        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val titleTextView = TextView(this).apply {
            text = context.getString(R.string.all_tasks)
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
                    if (diffX > 0 && abs(p2) > swipeVelocityThreshold) {
                        finish()
                    } else {
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
            layoutManager = LinearLayoutManager(this@FullTaskViewActivity)
        }

        recyclerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        rootLayout.addView(titleTextView)
        rootLayout.addView(recyclerView)
        setContentView(rootLayout)

        loadAllTasks()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadAllTasks() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("courses")
            .get().addOnSuccessListener { courses ->
                var loadedCourses = 0

                for (courseDoc in courses) {
                    val courseId = courseDoc.id

                    db.collection("users").document(userId).collection("courses")
                        .document(courseId).collection("tasks").get()
                        .addOnSuccessListener { tasks ->
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

                                val distinctCourses = allTasks.map { it.courseId }.distinct()
                                for (courseId in distinctCourses) {
                                    db.collection("users").document(userId).collection("courses")
                                        .document(courseId).get()
                                        .addOnSuccessListener { doc ->
                                            val hex = doc.getString("color") ?: "#9E9E9E"
                                            CourseColorManager.setColorForCourse(courseId, hex)
                                        }
                                }

                                recyclerView.adapter = TaskAdapter(sortedTasks.toMutableList(), { task ->
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

                                    taskRef.delete().addOnSuccessListener {
                                        Toast.makeText(this, "Deleted ${task.taskName}", Toast.LENGTH_SHORT).show()
                                        allTasks.remove(task)
                                        recyclerView.adapter?.notifyDataSetChanged()
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

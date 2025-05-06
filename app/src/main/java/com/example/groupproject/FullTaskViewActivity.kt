package com.example.groupproject

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FullTaskViewActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val allTasks = mutableListOf<Task>()
    private val courseColors = mutableMapOf<String, Int>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val titleTextView = TextView(this).apply {
            text = "All Tasks"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 50, 0, 30)
        }

        recyclerView = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            layoutManager = LinearLayoutManager(this@FullTaskViewActivity)
        }

        rootLayout.addView(titleTextView)
        rootLayout.addView(recyclerView)

        setContentView(rootLayout)

        loadAllTasks()
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
                                recyclerView.adapter = TaskAdapter(allTasks, { task ->
                                    val taskRef = db.collection("users").document(userId)
                                        .collection("courses").document(task.courseId)
                                        .collection("tasks").document(task.taskName)

                                    taskRef.update("completed", true).addOnSuccessListener {
                                        Toast.makeText(this, "Marked ${task.taskName} as complete", Toast.LENGTH_SHORT).show()
                                        allTasks.clear()
                                        loadAllTasks()
                                    }
                                }, courseColors)
                            }
                        }
                }
            }
    }
}

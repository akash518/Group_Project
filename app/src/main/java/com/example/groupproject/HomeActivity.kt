package com.example.groupproject

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var progressRings: ProgressView
    private lateinit var progressText: TextView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homescreen)

        progressRings = findViewById(R.id.progressRings)
        progressText = findViewById(R.id.progressText)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInWithEmailAndPassword("test@example.com", "test1234")
                .addOnSuccessListener {
                    Log.w("HomeActivity", "Starting")
                    loadProgressFromFirebase()
                }
                .addOnFailureListener { e ->
                    Log.e("HomeActivity", "Sign-in error: ${e.message}")
                }
        } else {
            Log.w("HomeActivity", "Else")
            loadProgressFromFirebase()
        }
    }

    private fun loadProgressFromFirebase() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
//        val userId = "uid"
        val courseRef = db.collection("users").document(userId).collection("courses")

        courseRef.get().addOnSuccessListener { courseDocs ->
            Log.d("HomeActivity", "Found ${courseDocs.size()} courses.")

            val courseProgressList = mutableListOf<CourseProgress>()
            val totalCourses = courseDocs.size()
            var loadedCourses = 0

            var totalTasks = 0
            var completedTasks = 0

            for (courseDoc in courseDocs) {
                val courseId = courseDoc.id
                val taskRef = courseRef.document(courseId).collection("tasks")

                taskRef.get().addOnSuccessListener { tasks ->
                    val thisCourseTotal = tasks.size()
                    val thisCourseCompleted = tasks.count { it.getBoolean("completed") == true }

                    totalTasks += thisCourseTotal
                    completedTasks += thisCourseCompleted

                    Log.d("HomeActivity", "Course $courseId has $thisCourseTotal tasks, $thisCourseCompleted completed")

                    val progress = if (thisCourseTotal > 0) {
                        thisCourseCompleted.toFloat() / thisCourseTotal
                    } else 0f

                    courseRef.document(courseId).update("progress", progress)
                    courseProgressList.add(CourseProgress(courseId, progress))

                    loadedCourses++
                    if (loadedCourses == totalCourses) {
                        Log.d("HomeActivity", "Finished loading all courses")
                        progressRings.updateProgress(courseProgressList)

                        val percent = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0
                        val percentStr = "$percent%"
                        val detailStr = "\n$completedTasks/$totalTasks\nComplete"

                        val spannable = android.text.SpannableString(percentStr + detailStr)
                        spannable.setSpan(
                            android.text.style.RelativeSizeSpan(1.8f), // 1.8x larger
                            0,
                            percentStr.length,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        progressText.text = spannable

                    }
                }
                    .addOnFailureListener { e ->
                        Log.e("HomeActivity", "Firestore query failed: ${e.message}", e)
                    }
            }

        }
    }
}

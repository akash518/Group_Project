package com.example.groupproject

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.installations.installations
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.log

class HomeController(private val context: Context, private val model: HomeModel, private val view: HomeView) {
    private var selectedCourseId: String? = null

    init {
        model.setController(this)
    }

    fun initialize() {
        Log.d("DEBUG", "Controller.initialize() called")
        checkUserAuth()
    }

    private fun checkUserAuth() {
        Log.d("DEBUG", "checkUserAuthentication() called")
        val prefs = context.getSharedPreferences("TaskTrackerPrefs", Context.MODE_PRIVATE)
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        Log.d("DEBUG", "User: ${user?.uid}")
        Log.d("DEBUG", "accountSetupComplete: ${prefs.getBoolean("accountSetupComplete", false)}")
        Log.d("DEBUG", "saved email: ${prefs.getString("email", null)}")

        if (user == null) {
            showLoginDialog()
            return
        }

        user.getIdToken(true).addOnCompleteListener { token ->
            if (token.isSuccessful) {
                val setupComplete = prefs.getBoolean("accountSetupComplete", false)
                val userId = user.uid

                if (!setupComplete) {
                    validateUser(userId, prefs, auth)
                } else {
                    model.loadProgress()
                }
            } else {
                logOut()
            }
        }
    }

    private fun validateUser(userId: String, prefs: android.content.SharedPreferences, auth: FirebaseAuth) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val isValid = doc.exists() && doc.getString("email") != null

                if (!isValid) {
                    logOut()
                } else {
                    prefs.edit().putBoolean("accountSetupComplete", true).apply()
                    model.loadProgress()
                }
            }
            .addOnFailureListener {
                view.showError("Failed to check user info")
            }
    }

    private fun showLoginDialog() {
        view.showLoginDialog {
            initialize()
        }
    }

    fun handleData(courses: List<CourseProgress>, tasks: List<Task>) {
        updateSpinner(courses)
        updateView()
    }

    fun handleError(message: String) {
        view.showError(message)
    }

    fun handleCompletedTask() {
        model.loadProgress() // Reload data to refresh progress
        view.showSuccess("Task marked as complete")
    }

    fun handleAuth() {
        showLoginDialog()
    }

    private fun updateSpinner(courses: List<CourseProgress>) {
        val spinnerItems = mutableListOf("All Courses")
        spinnerItems.addAll(courses.map { it.courseId }.filter {
            it != "placeholder" && it != "No Courses"
        })
        view.updateCourseSpinner(spinnerItems)
    }

    private fun updateView() {
        view.updateDateRange(model.getDateRange())

        val filteredData = model.getFilteredTasks(selectedCourseId)

        view.updateProgress(
            progress = filteredData.weeklyProgress,
            selectedCourseId = selectedCourseId,
            completedCount = filteredData.completed,
            totalCount = filteredData.total
        )

        view.updateTaskList(filteredData.tasks)
    }

    private fun onAuthSuccess() {
        model.loadProgress()
    }

    fun onCourseSelected(courseId: String?) {
        selectedCourseId = courseId
        updateView()
    }

    fun onSwipeLeft() {
        model.goToNextWeek()
        updateView()
    }

    fun onSwipeRight() {
        model.goToPreviousWeek()
        updateView()
    }

    fun onTaskCompleted(task: Task) {
        model.markTaskComplete(task)
    }

    fun onAddCourseClicked() {
        view.getAdController().showAdBeforeAddingCourse(context as Activity) {
            view.showCourseCreationDialog {
                model.loadProgress()
            }
        }
    }

    fun onCreateTaskClicked() {
        val courses = getCourseList()
        view.getAdController().showAdBeforeAddingTask(context as Activity) {
            view.showTaskCreationDialog(courses) {
                model.loadProgress()
            }
        }
    }

    fun logOut() {
        FirebaseAuth.getInstance().signOut()
        val prefs = context.getSharedPreferences("TaskTrackerPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        showLoginDialog()
    }

    fun onPomodoroNavigation() {
        val intent = Intent(context, PomodoroActivity::class.java)
        view.navigateToActivity(intent, R.anim.slide_in_right, R.anim.slide_out_left)
    }

    fun onFullTasksNavigation() {
        val intent = Intent(context, FullTaskViewActivity::class.java)
        view.navigateToActivity(intent, R.anim.slide_in_left, R.anim.slide_out_right)
    }

    fun onManageCoursesNavigation() {
        val intent = Intent(context, ManageCourses::class.java)
        view.navigateToActivity(intent, R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun getCourseList(): List<String> {
        return model.getCourseProgressList().map { it.courseId }.filter {
            it != "No Courses" && it != "placeholder" && it.isNotBlank()
        }
    }
}

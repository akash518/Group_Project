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

/**
 * Controller for Home screen.
 * Handles user authentication, gesture actions, course/task interactions, and passes results between model and view.
 */
class HomeController(private val context: Context, private val model: HomeModel, private val view: HomeView) {
    private var selectedCourseId: String? = null

    init {
        // Let the model know which controller it should call back to
        model.setController(this)
    }

    /** Controller initialization */
    fun initialize() {
        Log.d("DEBUG", "Controller.initialize() called")
        checkUserAuth()
    }

    /** Checks if user is authenticated and whether the account setup is complete */
    private fun checkUserAuth() {
        Log.d("DEBUG", "checkUserAuthentication() called")
        val prefs = context.getSharedPreferences("TaskTrackerPrefs", Context.MODE_PRIVATE)
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        Log.d("DEBUG", "User: ${user?.uid}")
        Log.d("DEBUG", "accountSetupComplete: ${prefs.getBoolean("accountSetupComplete", false)}")
        Log.d("DEBUG", "saved email: ${prefs.getString("email", null)}")

        // If user is not logged in, show login dialog
        if (user == null) {
            showLoginDialog()
            return
        }

        // Refresh ID token and proceed if valid
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

    /** Validates if the Firestore user doc exists - logs out if not */
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

    /** Shows login dialog and reinitializes controller once logged in */
    private fun showLoginDialog() {
        view.showLoginDialog {
            initialize()
        }
    }

    /** Called by model when data is ready */
    fun handleData(courses: List<CourseProgress>, tasks: List<Task>) {
        updateSpinner(courses)
        updateView()
    }

    /** Shows any errors from the model */
    fun handleError(message: String) {
        view.showError(message)
    }

    /** Called when a task is marked completed */
    fun handleCompletedTask() {
        model.loadProgress() // Reload data to refresh progress
        view.showSuccess("Task marked as complete")
    }

    /** Called when a task is deleted */
    fun handleDeletedTask(task: String) {
        model.loadProgress()
        view.showSuccess("Deleted $task")
    }

    /** Re-authenticate the user if needed */
    fun handleAuth() {
        showLoginDialog()
    }

    /** Populates the course spinner in the UI with available courses */
    private fun updateSpinner(courses: List<CourseProgress>) {
        val spinnerItems = mutableListOf("All Courses")
        // Filter out placeholder and sort courses alphabetically
        val sortedCourseIds = courses.map { it.courseId }
            .filter { it != "placeholder" && it != "No Courses" }
            .sorted()
        spinnerItems.addAll(sortedCourseIds)
        view.updateCourseSpinner(spinnerItems)
    }

    /** Updates progress bars, task list, and date range in the UI */
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
        model.checkAndSendReminders()
    }

    /** Called when the user selects a course from the spinner */
    fun onCourseSelected(courseId: String?) {
        selectedCourseId = courseId
        updateView()
    }

    /** User swiped to go to next week */
    fun onSwipeLeft() {
        model.goToNextWeek()
        updateView()
    }

    /** User swiped to go to previous week */
    fun onSwipeRight() {
        model.goToPreviousWeek()
        updateView()
    }

    /** Mark a task as completed */
    fun onTaskCompleted(task: Task) {
        model.markTaskComplete(task)
    }

    /** Delete a task */
    fun onTaskDeleted(task: Task) {
        model.deleteTask(task)
    }

    /** Show course creation dialog after ad when applicable */
    fun onAddCourseClicked() {
        view.getAdController().showAdBeforeAddingCourse(context as Activity) {
            view.showCourseCreationDialog {
                model.loadProgress()
            }
        }
    }

    /** Show task creation dialog after ad when applicable */
    fun onCreateTaskClicked() {
        val courses = getCourseList()
        view.getAdController().showAdBeforeAddingTask(context as Activity) {
            view.showTaskCreationDialog(courses) {
                model.loadProgress()
            }
        }
    }

    /** Log the user out and clear saved preferences */
    fun logOut() {
        FirebaseAuth.getInstance().signOut()
        val prefs = context.getSharedPreferences("TaskTrackerPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        showLoginDialog()
    }

    /** Navigates to the Pomodoro activity */
    fun onPomodoroNavigation() {
        val intent = Intent(context, PomodoroActivity::class.java)
        view.navigateToActivity(intent, R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /** Navigates to the full task view screen */
    fun onFullTasksNavigation() {
        val intent = Intent(context, FullTaskViewActivity::class.java)
        view.navigateToActivity(intent, R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /** Navigates to the course management screen */
    fun onManageCoursesNavigation() {
        val intent = Intent(context, ManageCourses::class.java)
        view.navigateToActivity(intent, R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /** Get a list of course IDs for task creation dialog */
    private fun getCourseList(): List<String> {
        return model.getCourseProgressList().map { it.courseId }
            .filter { it != "No Courses" && it != "placeholder" && it.isNotBlank() }
            .sorted()
    }

    /** Forces a data refresh and reminder check */
    fun refresh() {
        model.loadProgress()
        model.checkAndSendReminders()
    }
}

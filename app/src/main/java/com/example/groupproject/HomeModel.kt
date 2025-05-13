package com.example.groupproject

import android.content.Context
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Model for Home screen
 * Handles Firebase data fetching, task filtering, progress calculation, and reminder emails.
 */
class HomeModel(private val context: Context) {
    private lateinit var controller: HomeController

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val allTasks = mutableListOf<Task>() // List of all tasks loaded from Firebase
    private var courseProgressList = listOf<CourseProgress>() // List of course progress percentages
    private var currentStartDate: Calendar = getStartOfWeek(Calendar.getInstance()) // Sunday of current week

    /** Connect controller */
    fun setController(controller: HomeController) {
        this.controller = controller
    }

    /** Loads all courses and tasks, calculates progress, and updates controller */
    fun loadProgress() {
        Log.d("DEBUG", "loadProgressFromFirebase() called")
        val userId = auth.currentUser?.uid
        if (userId == null) {
            controller.handleAuth()
            return
        }

        val courseRef = db.collection("users").document(userId).collection("courses")
        courseRef.get().addOnSuccessListener { courseDocs ->
            allTasks.clear()
            courseProgressList = listOf()

            // No courses exist
            if (courseDocs.isEmpty) {
                courseProgressList = listOf(CourseProgress("No Courses", 0f))
                allTasks.clear()
                CourseColorManager.reset()
                controller.handleData(courseProgressList, allTasks)
                return@addOnSuccessListener
            }

            val progressList = mutableListOf<CourseProgress>()
            val totalCourses = courseDocs.size()
            var loadedCourses = 0

            // For each course, load its tasks and calculate progress
            for (courseDoc in courseDocs) {
                val courseId = courseDoc.id
                val taskRef = courseRef.document(courseId).collection("tasks")
                val colorHex = courseDoc.getString("color") ?: "#9E9E9E"
                CourseColorManager.setColorForCourse(courseId, colorHex)

                taskRef.get().addOnSuccessListener { tasks ->
                    val thisCourseTotal = tasks.size()
                    val thisCourseCompleted = tasks.count { it.getBoolean("completed") == true }
                    val progress = if (thisCourseTotal > 0) {
                        thisCourseCompleted.toFloat() / thisCourseTotal
                    } else 0f

                    courseRef.document(courseId).update("progress", progress)
                    progressList.add(CourseProgress(courseId, progress))

                    // Build task objects
                    for (task in tasks) {
                        val taskName = task.id
                        val timestamp = task.getTimestamp("dueDate")
                        val completed = task.getBoolean("completed") == true
                        val dueDate = timestamp?.toDate()
                        val formatted = if (dueDate != null) {
                            "Due " + SimpleDateFormat("MMM d 'at' h:mm a", Locale.US).format(dueDate)
                        } else {
                            "No due date"
                        }

                        allTasks.add(Task(courseId, taskName, completed, formatted, dueDate))
                    }

                    loadedCourses++
                    if (loadedCourses == totalCourses) {
                        Log.d("HomeModel", "Finished loading all courses")
                        courseProgressList = progressList
                        controller.handleData(courseProgressList, allTasks)
                    }
                }.addOnFailureListener { e ->
                    controller.handleError("Failed to load tasks: ${e.message}")
                }
            }
        }.addOnFailureListener { e ->
            controller.handleError("Failed to load tasks: ${e.message}")
        }
    }

    /** Marks a task as completed in Firebase */
    fun markTaskComplete(task: Task) {
        val userId = auth.currentUser?.uid ?: return
        val taskRef = db.collection("users").document(userId)
            .collection("courses").document(task.courseId)
            .collection("tasks").document(task.taskName)

        taskRef.update("completed", true)
            .addOnSuccessListener { controller.handleCompletedTask() }
            .addOnFailureListener { controller.handleError("Failed to update task") }
    }

    /** Deletes a task and updates controller */
    fun deleteTask(task: Task) {
        val userId = auth.currentUser?.uid ?: return
        val taskRef = db.collection("users").document(userId)
            .collection("courses").document(task.courseId)
            .collection("tasks").document(task.taskName)

        taskRef.delete()
            .addOnSuccessListener {
                allTasks.remove(task)
                controller.handleDeletedTask(task.taskName)
            }
            .addOnFailureListener {
                controller.handleError("Failed to delete ${task.taskName}")
            }
    }

    // Helper methods
    fun getCourseProgressList(): List<CourseProgress> = courseProgressList
    fun goToPreviousWeek() { currentStartDate.add(Calendar.DATE, -7)}
    fun goToNextWeek() { currentStartDate.add(Calendar.DATE, 7)}

    /** Resets time to start of week (Sunday at 12:00 AM) */
    private fun getStartOfWeek(date: Calendar): Calendar {
        val copy = date.clone() as Calendar
        copy.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        copy.set(Calendar.HOUR_OF_DAY, 0)
        copy.set(Calendar.MINUTE, 0)
        copy.set(Calendar.SECOND, 0)
        copy.set(Calendar.MILLISECOND, 0)
        return copy
    }

    /** Returns a string like for current week */
    fun getDateRange(): String {
        val sdf = SimpleDateFormat("MMM d", Locale.US)
        val start = sdf.format(currentStartDate.time)
        val endDate = currentStartDate.clone() as Calendar
        endDate.add(Calendar.DATE, 6)
        val end = sdf.format(endDate.time)
        return "$start to $end"
    }

    /**
     * Filters tasks by week and optionally by course.
     * Calculates completion progress for that week.
     */
    fun getFilteredTasks(selectedCourseId: String?): FilteredTasks {
        val isAllCourses = selectedCourseId == null
        val selectedStart = currentStartDate.time
        val selectedEnd = (currentStartDate.clone() as Calendar).apply {
            add(Calendar.DATE, 6)
        }.time

        // All tasks due within this week
        val weeklyTasks = allTasks.filter { task ->
            task.dueDate != null && task.dueDate.after(selectedStart) && task.dueDate.before(selectedEnd)
        }

        // Filtered further by course and exclude completed tasks
        val filteredTasks = weeklyTasks.filter { task ->
            (isAllCourses || task.courseId == selectedCourseId) && !task.isCompleted
        }

        // Sort tasks
        val now = Calendar.getInstance().time
        val sortedTasks = filteredTasks.sortedWith(compareBy<Task> {
            when {
                !it.isCompleted && it.dueDate != null && it.dueDate.before(now) -> 0 // Overdue
                !it.isCompleted -> 1 // In Progress
                else -> 2 // Completed
            }
        }.thenBy { it.dueDate })

        // Map for progress: courseId -> (completedCount, totalCount)
        val courseData = mutableMapOf<String, Pair<Int, Int>>()

        for (task in weeklyTasks) {
            val current = courseData[task.courseId]?: (0 to 0)
            val updated = if (task.isCompleted) {
                Pair(current.first + 1, current.second + 1)
            } else {
                Pair(current.first, current.second + 1)
            }
            courseData[task.courseId] = updated
        }

        // Update progress bar values for each course
        val updatedProgress = courseProgressList.map {course ->
            val (completed, total) = courseData[course.courseId]?: (0 to 0)
            val progress = if (total > 0) completed.toFloat() / total else 0f
            CourseProgress(course.courseId, progress)
        }

        val (completed, total) = if (isAllCourses) {
            courseData.values.fold(0 to 0) { acc, pair ->
                (acc.first + pair.first) to (acc.second + pair.second)
            }
        } else {
            courseData[selectedCourseId]?: (0 to 0)
        }

        return FilteredTasks(
            tasks = sortedTasks,
            weeklyProgress = updatedProgress,
            completed = completed,
            total = total
        )
    }

    /** Sends email reminders for tasks due within 24 hours (only once per task) */
    fun checkAndSendReminders() {
        val now = Calendar.getInstance().time
        val nextDay = (Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 24) }).time
        val userEmail = auth.currentUser?.email?: return
        val tasks = getFilteredTasks(null).tasks

        for (task in tasks) {
            if (!task.isCompleted && task.dueDate != null && task.dueDate.after(now) && task.dueDate.before(nextDay)) {
                val taskRef = db.collection("users")
                    .document("courses")
                    .collection("courses")
                    .document(task.courseId)
                    .collection("tasks")
                    .document(task.taskName)

                taskRef.get().addOnSuccessListener { doc ->
                    val reminderSent = doc.getBoolean("reminderSent") ?: false

                    if (!reminderSent) {
                        EmailUtils().sendEmailReminder(
                            email = userEmail,
                            subject = "Reminder: \"${task.taskName}\" is due soon for ${task.courseId}",
                            text = """
                        Hi,

                        Just a quick reminder that your assignment:

                        📚 Course: ${task.courseId}
                        📌 Task: ${task.taskName}
                        📅 Due: ${task.dueDateFormatted}

                        is coming up soon. Make sure to stay on track and submit it on time!

                        Good luck and keep up the great work.

                        — Task Tracker App
                    """.trimIndent()
                        )
                        taskRef.update("reminderSent", true)
                    }
                }
            }
        }
    }
}

/** Wrapper data class returned from getFilteredTasks() */
data class FilteredTasks(
    val tasks: List<Task>,
    val weeklyProgress: List<CourseProgress>,
    val completed: Int,
    val total: Int
)
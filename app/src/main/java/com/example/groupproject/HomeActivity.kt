package com.example.groupproject

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.ViewOverlay
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class HomeActivity : AppCompatActivity() {

    private lateinit var menu: ImageButton
    private lateinit var progressRings: ProgressView
    private lateinit var progressText: TextView
    private lateinit var courseSpinner: Spinner
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var dateRange: TextView
    private lateinit var leftArrow: TextView
    private lateinit var rightArrow: TextView
    private lateinit var addCourse: Button
    private lateinit var createTask: Button
    private var userId: String? = null


    private var currentStartDate: Calendar = getStartOfWeek(Calendar.getInstance())
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var courseProgressList = listOf<CourseProgress>()
    private val allTasks = mutableListOf<Task>()
//    private val courseColors = mutableMapOf<String, Int>()
//    private val colorPalette = listOf(
//        Color.parseColor("#E53935"), // Red
//        Color.parseColor("#FB8C00"), // Orange
//        Color.parseColor("#43A047"), // Green
//        Color.parseColor("#1E88E5"), // Blue
//        Color.parseColor("#8E24AA"), // Purple
//        Color.parseColor("#00897B"), // Teal
//        Color.parseColor("#FDD835"), // Yellow
//        Color.parseColor("#795548"),   // brown
//        Color.parseColor("#607D8B")    // gray-blue
//    )


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homescreen)

        menu = findViewById(R.id.menu)
        dateRange = findViewById(R.id.dateRange)
//        leftArrow = findViewById(R.id.leftArrow)
//        rightArrow = findViewById(R.id.rightArrow)
        courseSpinner = findViewById(R.id.courses)
        progressRings = findViewById(R.id.progressRings)
        progressText = findViewById(R.id.progressText)
        taskRecyclerView = findViewById(R.id.taskView)
        taskRecyclerView.layoutManager = LinearLayoutManager(this)
        addCourse = findViewById<Button>(R.id.addCourse)
        createTask = findViewById<Button>(R.id.createTask)

        val userId = auth.currentUser?.uid
        val prefs = getSharedPreferences("TaskTrackerPrefs", MODE_PRIVATE)
        if (userId != null) {
            val setupComplete = prefs.getBoolean("accountSetupComplete", false)
            Log.d("Account", userId)
            Log.d("Account", setupComplete.toString())
            if (!setupComplete) {
                Log.d("Account", userId)
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { doc ->
                        val isValid = doc.exists() && doc.getString("email") != null

                        if (!isValid) {
                            startActivity(Intent(this, CreateAccount::class.java))
                            finish()
                        } else {
                            prefs.edit().putBoolean("accountSetupComplete", true).apply()
                        }
                        loadProgressFromFirebase()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to check user info", Toast.LENGTH_SHORT).show()
                    }
            }
            loadProgressFromFirebase()
        } else {
            startActivity(Intent(this, CreateAccount::class.java))
            finish()
            loadProgressFromFirebase()
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
                        if (diffX > 0) swipeToPreviousWeek() else swipeToNextWeek()
                        return true
                    }
                }
                return false
            }
        })

        val fullTaskViewGestureDetector = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener() {
            private val swipeThreshold = 100
            private val swipeVelocityThreshold = 100

            override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
                if (p0 == null) return false
                val diffX = p1.x - p0.x
                val diffY = p1.y - p0.y
                if (abs(diffX) > swipeThreshold && abs(p2) > swipeVelocityThreshold) {
                    val intent = Intent(this@HomeActivity, FullTaskViewActivity::class.java)
                    startActivity(intent)
                    return true
                }
                return false
            }
        })

        menu.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.home_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_full_tasks -> {
                        startActivity(Intent(this, FullTaskViewActivity::class.java))
                        true
                    }
                    R.id.menu_logout -> {
                        FirebaseAuth.getInstance().signOut()

                        val prefs = getSharedPreferences("TaskTrackerPrefs", MODE_PRIVATE)
                        prefs.edit().clear().apply()

                        startActivity(Intent(this, CreateAccount::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        dateRange.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_MOVE) {
                v.performClick()
            }
            true
        }

        Log.d("PrefsCheck", "Setup complete: ${prefs.getBoolean("accountSetupComplete", false)}, " +
                "Email: ${prefs.getString("email", null)}")

        courseSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = parent.getItemAtPosition(position).toString()
                val selectedId = if (selected == "All Courses") null else selected
                updateView(selectedId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        updateDateRange()

        addCourse.setOnClickListener {
            CourseCreation(this) {
                loadProgressFromFirebase()
            }.show()

//            val selected = courseSpinner.selectedItem.toString()
//            val selectedId = if (selected == "All Courses") null else selected
//            updateView(selectedId)
        }

        createTask.setOnClickListener {
            val courses = courseProgressList.map { it.courseId }.filter {
                it != "No Courses" && it.isNotBlank()
            }

            TaskCreation(this, courses) {
                loadProgressFromFirebase()
            }.show()

//            val selected = courseSpinner.selectedItem.toString()
//            val selectedId = if (selected == "All Courses") null else selected
//            updateView(selectedId)
        }

        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener { _, event ->
            fullTaskViewGestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun loadProgressFromFirebase() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val courseRef = db.collection("users").document(userId).collection("courses")

        courseRef.get().addOnSuccessListener { courseDocs ->
            Log.d("HomeActivity", "Found ${courseDocs.size()} courses.")
            allTasks.clear()
            courseProgressList = listOf()

            if (courseDocs.isEmpty) {
                Log.d("HomeActivity", "No courses found — showing default gray ring")

                courseProgressList = listOf(CourseProgress("No Courses", 0f))
                allTasks.clear()
                CourseColorManager.reset()

                val adapter = ArrayAdapter(this, R.layout.spinner_item, listOf("All Courses"))
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                courseSpinner.adapter = adapter

                updateView(null)
                return@addOnSuccessListener
            }

            val progressList = mutableListOf<CourseProgress>()
            val totalCourses = courseDocs.size()
            var loadedCourses = 0

//            var totalTasks = 0
//            var completedTasks = 0

            for (courseDoc in courseDocs) {
                val courseId = courseDoc.id
                val taskRef = courseRef.document(courseId).collection("tasks")
                val colorHex = courseDoc.getString("color") ?: "#9E9E9E"
                CourseColorManager.setColorForCourse(courseId, colorHex)


                taskRef.get().addOnSuccessListener { tasks ->
                    val thisCourseTotal = tasks.size()
                    val thisCourseCompleted = tasks.count { it.getBoolean("completed") == true }

//                    totalTasks += thisCourseTotal
//                    completedTasks += thisCourseCompleted

                    Log.d("HomeActivity", "Course $courseId has $thisCourseTotal tasks, $thisCourseCompleted completed")

                    val progress = if (thisCourseTotal > 0) {
                        thisCourseCompleted.toFloat() / thisCourseTotal
                    } else 0f

                    courseRef.document(courseId).update("progress", progress)
                    progressList.add(CourseProgress(courseId, progress))
//                    courseData[courseId] = Pair(thisCourseCompleted, thisCourseTotal)

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
                        Log.d("HomeActivity", "Finished loading all courses")
                        courseProgressList = progressList

//                        courseColors.clear()
//                        for ((index, course) in courseProgressList.withIndex()) {
//                            if (!courseColors.containsKey(course.courseId)) {
//                                val color = colorPalette[index % colorPalette.size]
//                                courseColors[course.courseId] = color
//                            }
//                        }

                        val spinnerItems = mutableListOf("All Courses")
                        spinnerItems.addAll(courseProgressList.map {it.courseId})
                        val adapter = ArrayAdapter(this, R.layout.spinner_item, spinnerItems)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        courseSpinner.adapter = adapter
                        updateView(null)
                    }
                }.addOnFailureListener { e ->
                        Log.e("HomeActivity", "Firestore query failed: ${e.message}", e)
                    }
            }
        }
    }

    private fun updateView(selectedCourseId: String?) {
        val isAllCourses = selectedCourseId == null
        val selectedStart = currentStartDate.time
        val selectedEnd = (currentStartDate.clone() as Calendar).apply {
            add(Calendar.DATE, 6)
        }.time

        val weeklyTasks = allTasks.filter { task ->
            task.dueDate != null &&
                    task.dueDate.after(selectedStart) &&
                    task.dueDate.before(selectedEnd)
        }

        val filteredTasks = weeklyTasks.filter { task ->
            (isAllCourses || task.courseId == selectedCourseId) &&
                    !task.isCompleted
//                    task.dueDate != null &&
//                    task.dueDate.after(selectedStart) &&
//                    task.dueDate.before(selectedEnd)
        }

        val now = Calendar.getInstance().time
        val sortedTasks = filteredTasks.sortedWith(compareBy<Task> {
            when {
                !it.isCompleted && it.dueDate != null && it.dueDate.before(now) -> 0 // Overdue
                !it.isCompleted -> 1 // In Progress
                else -> 2 // Completed
            }
        }.thenBy { it.dueDate })
//        Log.d("DateRange", "Filtered: " + filteredTasks.toString())
//        Log.d("DateRange", "All: " + allTasks.toString())
        Log.d("DateRange", "Courses: " + courseProgressList.toString())
        Log.d("DateRange", "Weekly: " + weeklyTasks.toString())
//        if (filteredTasks.size > 0) {
//            Log.d("DateRange", filteredTasks[0].dueDate?.after(selectedStart).toString())
//            Log.d("DateRange", filteredTasks[0].dueDate?.before(selectedEnd).toString())
//        }
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
        Log.d("DateRange", "Data: " + courseData.toString())

        val updatedProgress = courseProgressList.map {course ->
            val (completed, total) = courseData[course.courseId]?: (0 to 0)
            val progress = if (total > 0) completed.toFloat() / total else 0f
            CourseProgress(course.courseId, progress)
        }
        Log.d("DateRange", "Updated: " + updatedProgress.toString())

        progressRings.setCourseColors(CourseColorManager.getAllColors())
        progressRings.updateProgress(updatedProgress)
        progressRings.setSelectedCourse(selectedCourseId)

        val (completed, total) = if (isAllCourses) {
            courseData.values.fold(0 to 0) { acc, pair ->
                (acc.first + pair.first) to (acc.second + pair.second)
            }
        } else {
            courseData[selectedCourseId]?: (0 to 0)
        }

        val percent = if (total > 0) (completed * 100 / total) else 0
        val percentStr = "$percent%"
        val detailStr = "\n$completed/$total\nComplete"

        val spannable = android.text.SpannableString(percentStr + detailStr)
        spannable.setSpan(
            android.text.style.RelativeSizeSpan(1.8f),
            0,
            percentStr.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        progressText.text = spannable

        taskRecyclerView.adapter = TaskAdapter(sortedTasks.toMutableList(), { task ->
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@TaskAdapter
            val taskRef = FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("courses").document(task.courseId)
                .collection("tasks").document(task.taskName)

            taskRef.update("completed", true)
                .addOnSuccessListener {
                    Toast.makeText(this, "Marked '${task.taskName}' as complete", Toast.LENGTH_SHORT).show()
                    loadProgressFromFirebase() // refresh everything
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show()
                }
        }, CourseColorManager.getAllColors(), { task ->
            userId?.let { uid ->
                val taskRef = db.collection("users").document(uid)
                    .collection("courses").document(task.courseId)
                    .collection("tasks").document(task.taskName)

                taskRef.delete().addOnSuccessListener {
                    Toast.makeText(this, "Deleted ${task.taskName}", Toast.LENGTH_SHORT).show()
                    allTasks.remove(task)
                    taskRecyclerView.adapter?.notifyDataSetChanged()
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to delete ${task.taskName}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun getStartOfWeek(date: Calendar): Calendar {
        val copy = date.clone() as Calendar
        copy.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        copy.set(Calendar.HOUR_OF_DAY, 0)
        copy.set(Calendar.MINUTE, 0)
        copy.set(Calendar.SECOND, 0)
        copy.set(Calendar.MILLISECOND, 0)
        return copy
    }

    private fun updateDateRange() {
        val sdf = SimpleDateFormat("MMM d", Locale.US)
        val start = sdf.format(currentStartDate.time)
        val endDate = currentStartDate.clone() as Calendar
        endDate.add(Calendar.DATE, 6)
        val end = sdf.format(endDate.time)

        dateRange.text = "$start to $end"
    }

    private fun swipeToPreviousWeek() {
        currentStartDate.add(Calendar.DATE, -7)
        updateDateRange()
        val selected = courseSpinner.selectedItem.toString()
        val selectedId = if (selected == "All Courses") null else selected
        updateView(selectedId)
    }

    private fun swipeToNextWeek() {
        currentStartDate.add(Calendar.DATE, 7)
        updateDateRange()
        val selected = courseSpinner.selectedItem.toString()
        val selectedId = if (selected == "All Courses") null else selected
        updateView(selectedId)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

}

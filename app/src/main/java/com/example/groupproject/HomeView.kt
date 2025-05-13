package com.example.groupproject

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * View for the Home screen.
 * Initializes UI components, handles user interaction, and communicates with the HomeController.
 */
class HomeView : AppCompatActivity() {
    // Home screen components
    private lateinit var controller: HomeController
    private lateinit var model: HomeModel
    private lateinit var adController: AdController
    private lateinit var menu: ImageButton
    private lateinit var progressRings: ProgressView
    private lateinit var progressText: TextView
    private lateinit var courseSpinner: Spinner
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var dateRange: TextView
    private lateinit var addCourse: Button
    private lateinit var createTask: Button
    // Gesture detectors
    private lateinit var dateRangeGestureDetector: GestureDetector
    private lateinit var viewGestureDetector: GestureDetector

    private var firstResume = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homescreen)

        model = HomeModel()
        controller = HomeController(this, model, this)
        adController = AdController(this)

        initializeViews()
        setupGestureDetectors()
        setupEventListeners()

        controller.initialize()
        adController.handleAppLaunch(this)
    }

    private fun initializeViews() {
        menu = findViewById(R.id.menu)
        dateRange = findViewById(R.id.dateRange)
        courseSpinner = findViewById(R.id.courses)
        progressRings = findViewById(R.id.progressRings)
        progressText = findViewById(R.id.progressText)
        taskRecyclerView = findViewById(R.id.taskView)
        taskRecyclerView.layoutManager = LinearLayoutManager(this)
        addCourse = findViewById(R.id.addCourse)
        createTask = findViewById(R.id.createTask)
    }

    /** Sets up swipe gestures for date range and full-view transitions */
    private fun setupGestureDetectors() {
        dateRangeGestureDetector = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener() {
            private val swipeThreshold = 100
            private val swipeVelocityThreshold = 100

            override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
                if (p0 == null) return false
                val diffX = p1.x - p0.x
                val diffY = p1.y - p0.y
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > swipeThreshold && abs(p2) > swipeVelocityThreshold) {
                        if (diffX > 0) {
                            controller.onSwipeRight()
                        } else {
                            controller.onSwipeLeft()
                        }
                        return true
                    }
                }
                return false
            }
        })

        viewGestureDetector = GestureDetector(this, object: GestureDetector.SimpleOnGestureListener() {
            private val swipeVelocityThreshold = 100

            override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
                if (p0 == null) return false
                val diffX = p1.x - p0.x
                val diffY = p1.y - p0.y

                if (abs(diffX) > abs(diffY)) {
                    if (diffX > 0 && abs(p2) > swipeVelocityThreshold) {
                        controller.onPomodoroNavigation()
                    } else {
                        controller.onFullTasksNavigation()
                    }
                    return true
                }
                return false
            }
        })
    }

    /** Sets listeners for user actions */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupEventListeners() {
        menu.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.home_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_pomodoro -> {
                        controller.onPomodoroNavigation()
                        true
                    }
                    R.id.menu_full_tasks -> {
                        controller.onFullTasksNavigation()
                        true
                    }
                    R.id.menu_manage_courses -> {
                        controller.onManageCoursesNavigation()
                        true
                    }
                    R.id.menu_logout -> {
                        controller.logOut()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        // Date range swipe
        dateRange.setOnTouchListener { v, event ->
            dateRangeGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_MOVE) {
                v.performClick()
            }
            true
        }

        // Course spinner selection
        courseSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = parent.getItemAtPosition(position).toString()
                val selectedId = if (selected == "All Courses") null else selected
                controller.onCourseSelected(selectedId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Button clicks
        addCourse.setOnClickListener {
            controller.onAddCourseClicked()
        }

        createTask.setOnClickListener {
            controller.onCreateTaskClicked()
        }

        // Global gesture detection
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener { _, event ->
            viewGestureDetector.onTouchEvent(event)
            true
        }

        taskRecyclerView.setOnTouchListener { _, event ->
            viewGestureDetector.onTouchEvent(event)
            false
        }
    }

    // --Update View Functions--

    fun updateCourseSpinner(courses: List<String>) {
        // Create a dropdown adapter using the custom spinner layout and the course list
        val adapter = ArrayAdapter(this, R.layout.spinner_item, courses)
        // Set the layout to use for the dropdown menu of the spinner
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        courseSpinner.adapter = adapter
    }

    fun updateDateRange(dateRange: String) {
        this.dateRange.text = dateRange
    }

    /**
     * Updates the progress rings and central percentage display.
     * Called when task data is filtered or new data is loaded.
     * @param progress List of progress values for each course
     * @param selectedCourseId Currently selected course ID (or null for "All Courses")
     * @param completedCount Number of completed tasks this week
     * @param totalCount Total number of tasks this week
     */
    fun updateProgress(progress: List<CourseProgress>, selectedCourseId: String?, completedCount: Int, totalCount: Int) {
        // Update the color scheme for each course ring
        progressRings.setCourseColors(CourseColorManager.getAllColors())
        // Set the current progress percentages for the rings
        progressRings.updateProgress(progress)
        // Visually highlight the selected course ring
        progressRings.setSelectedCourse(selectedCourseId)

        // Calculate overall percentage of tasks completed this week
        val percent = if (totalCount > 0) (completedCount * 100 / totalCount) else 0
        val percentStr = "$percent%"
        val detailStr = "\n$completedCount/$totalCount\nComplete"

        // Combine into a single string
        val spannable = android.text.SpannableString(percentStr + detailStr)
        // Make the percentage value larger
        spannable.setSpan(
            android.text.style.RelativeSizeSpan(1.8f),
            0,
            percentStr.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        progressText.text = spannable
    }

    fun updateTaskList(tasks: List<Task>) {
        taskRecyclerView.adapter = TaskAdapter(tasks.toMutableList(),
            { task -> controller.onTaskCompleted(task) },
            CourseColorManager.getAllColors(),
            {task ->
                val index = tasks.indexOfFirst {
                    it.courseId == task.courseId && it.taskName == task.taskName
                }
                if (index != -1) {
                    controller.onTaskDeleted(task)
                    taskRecyclerView.adapter?.notifyItemRemoved(index)
                }
            }
        )
    }

    // --Dialog Functions--
    fun showLoginDialog(onSuccess: () -> Unit) {
        CreateAccount(onSuccess).show(supportFragmentManager, "CreateAccountDialog")
    }

    fun showCourseCreationDialog(onCourseAdded: () -> Unit) {
        CourseCreation(this, onCourseAdded).show()
    }

    fun showTaskCreationDialog(courses: List<String>, onTaskCreated: () -> Unit) {
        TaskCreation(this, courses, onTaskCreated).show()
    }

    // --Feedback Functions--
    fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Navigation function to implement transitions
    fun navigateToActivity(intent: Intent, enterAnim: Int, exitAnim: Int) {
        startActivity(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                enterAnim,
                exitAnim
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(enterAnim, exitAnim)
        }
    }

    fun getAdController(): AdController = adController

    /**
     * Automatically hides the soft keyboard when the user taps outside of an EditText field.
     * @param ev The motion event (e.g., a tap or swipe)
     * @return Boolean indicating whether the event was consumed
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            // Get the currently focused view
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                // Get the visible bounds of the EditText
                v.getGlobalVisibleRect(outRect)
                // If the tap is outside the bounds of the EditText
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    // Clear focus from the EditText
                    v.clearFocus()
                    // Hide the soft keyboard
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /** Refreshes view when returning from another activity */
    override fun onResume() {
        super.onResume()
        if (firstResume) {
            firstResume = false
            return
        }
        controller.refresh()
    }
}
package com.example.groupproject

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
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

class HomeView : AppCompatActivity() {
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

    private lateinit var dateRangeGestureDetector: GestureDetector
    private lateinit var viewGestureDetector: GestureDetector

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homescreen)

        model = HomeModel(this)
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
            private val swipeThreshold = 100
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

    // Public methods that the controller calls to update the view
    fun updateCourseSpinner(courses: List<String>) {
        val adapter = ArrayAdapter(this, R.layout.spinner_item, courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        courseSpinner.adapter = adapter
    }

    fun updateDateRange(dateRange: String) {
        this.dateRange.text = dateRange
    }

    fun updateProgress(progress: List<CourseProgress>, selectedCourseId: String?, completedCount: Int, totalCount: Int) {
        progressRings.setCourseColors(CourseColorManager.getAllColors())
        progressRings.updateProgress(progress)
        progressRings.setSelectedCourse(selectedCourseId)

        val percent = if (totalCount > 0) (completedCount * 100 / totalCount) else 0
        val percentStr = "$percent%"
        val detailStr = "\n$completedCount/$totalCount\nComplete"

        val spannable = android.text.SpannableString(percentStr + detailStr)
        spannable.setSpan(
            android.text.style.RelativeSizeSpan(1.8f),
            0,
            percentStr.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        progressText.text = spannable
    }

    fun updateTaskList(tasks: List<Task>) {
        taskRecyclerView.adapter = TaskAdapter(
            tasks.toMutableList(),
            { task -> controller.onTaskCompleted(task) },
            CourseColorManager.getAllColors()
        )
    }

    // Dialog methods
    fun showLoginDialog(onSuccess: () -> Unit) {
        CreateAccount(onSuccess).show(supportFragmentManager, "CreateAccountDialog")
    }

    fun showCourseCreationDialog(onCourseAdded: () -> Unit) {
        CourseCreation(this, onCourseAdded).show()
    }

    fun showTaskCreationDialog(courses: List<String>, onTaskCreated: () -> Unit) {
        TaskCreation(this, courses, onTaskCreated).show()
    }

    // Feedback methods
    fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

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

    // Touch handling for hiding keyboard
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

    override fun onResume() {
        super.onResume()
        controller.refresh()
    }
}
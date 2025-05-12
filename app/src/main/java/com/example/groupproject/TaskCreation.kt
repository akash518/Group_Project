package com.example.groupproject

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TaskCreation(private val context: Context, private val courseList: List<String>, private val onTaskCreated: () -> Unit) {
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.create_task, null)

        val task = view.findViewById<EditText>(R.id.taskName)
        val close = view.findViewById<Button>(R.id.close)
        val date = view.findViewById<TextInputEditText>(R.id.date)
        val time = view.findViewById<TextInputEditText>(R.id.time)
        val courseDropdown = view.findViewById<AutoCompleteTextView>(R.id.courseDropdown)
        val save = view.findViewById<Button>(R.id.save)

        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        val selectedDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 0)
        }

        val month = selectedDate.get(Calendar.MONTH) + 1
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)
        val year = selectedDate.get(Calendar.YEAR)
        date.setText("$month/$day/$year")
        time.setText("11:59 PM")

        val dialog = android.app.AlertDialog.Builder(context)
            .setView(view)
            .create()


        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, courseList)
        courseDropdown.setAdapter(adapter)

        fun updateButtonState() {
            val isValid = task.text?.isNotEmpty() == true &&
                    date.text?.isNotEmpty() == true &&
                    time.text?.isNotEmpty() == true &&
                    courseDropdown.text?.isNotEmpty() == true

            save.isEnabled = isValid
        }

        close.setOnClickListener {
            dialog.dismiss()
        }

        date.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(context, { _, y, m, d ->
                selectedDate.set(Calendar.YEAR, y)
                selectedDate.set(Calendar.MONTH, m)
                selectedDate.set(Calendar.DAY_OF_MONTH, d)
                date.setText("${m + 1}/$d/$y")
                updateButtonState()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }

        time.setOnClickListener {
            val now = Calendar.getInstance()
            TimePickerDialog(context, { _, hourOfDay, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDate.set(Calendar.MINUTE, minute)
                selectedDate.set(Calendar.SECOND, 59)
                selectedDate.set(Calendar.MILLISECOND, 0)

                val amPm = if (hourOfDay < 12) "AM" else "PM"
                val hour12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                time.setText(String.format("%02d:%02d %s", hour12, minute, amPm))
                updateButtonState()
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show()
        }

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updateButtonState()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        task.addTextChangedListener(watcher)
        date.addTextChangedListener(watcher)
        time.addTextChangedListener(watcher)
        courseDropdown.addTextChangedListener(watcher)

        courseDropdown.setOnClickListener {
            courseDropdown.showDropDown()
        }

        save.setOnClickListener {
            val title = task.text.toString().trim()
            val selectedCourse = courseDropdown.text.toString().trim()
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val taskMap = mapOf(
                "completed" to false,
                "dueDate" to Timestamp(selectedDate.time),
                "reminderSent" to false //added this for managing emails
            )

            db.collection("users").document(userId)
                .collection("courses").document(selectedCourse)
                .collection("tasks").document(title)
                .set(taskMap)
                .addOnSuccessListener {
                    Toast.makeText(context, "Task created", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    val prefs = context.getSharedPreferences("AdPrefs", Context.MODE_PRIVATE)
                    val added = prefs.getInt("tasksAdded", 0)
                    prefs.edit().putInt("tasksAdded", added+1).apply()
                    onTaskCreated()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to create task", Toast.LENGTH_SHORT).show()
                }
        }

        // Dismiss keyboard on outside touch
        dialog.setOnShowListener {
            val root = dialog.window?.decorView?.rootView
            root?.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val focusedView = dialog.currentFocus
                    if (focusedView is EditText) {
                        val outRect = Rect()
                        focusedView.getGlobalVisibleRect(outRect)
                        if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                            focusedView.clearFocus()
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
                        }
                    }
                }
                false
            }
        }

        dialog.show()
    }
}
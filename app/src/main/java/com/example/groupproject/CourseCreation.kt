package com.example.groupproject

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * This class handles the course creation dialog and saving the course to Firebase.
 * It assigns a color from a palette and ensures course uniqueness per user.
 * @param context The context to show dialogs and access SharedPreferences
 * @param onCourseAdded Callback function called when the course is successfully added
 */
class CourseCreation(private val context: Context, private val onCourseAdded: () -> Unit) {
    /**
     * Displays an AlertDialog where the user can input a new course name.
     * Handles color assignment, Firebase validation, and soft keyboard dismissal.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        // Inflate custom layout for the dialog
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.add_course, null)
        val inputField = dialogView.findViewById<EditText>(R.id.courseName)

        // Build the dialog
        val dialog = AlertDialog.Builder(context)
            .setTitle("Add New Course")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                // Grab the course name input and trim spaces
                val courseName = inputField.text.toString().trim()
                if (courseName.isEmpty()) {
                    Toast.makeText(context, "Course name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Get the current user ID from FirebaseAuth
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Firebase references
                val db = FirebaseFirestore.getInstance()
                val coursesRef = db.collection("users").document(userId).collection("courses")
                val colorPalette = CourseColorManager.colorPalette

                // Check for duplicates and assign color
                coursesRef.get().addOnSuccessListener { course ->
                    val courseExists = course.documents.any {doc ->
                        doc.id.equals(courseName, ignoreCase = true)
                    }
                    if (courseExists) {
                        Toast.makeText(context,"$courseName already exists", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Determine the document for the new course
                    val courseRef = coursesRef.document(courseName)

                    // Find all used colors and pick the next available
                    val usedColors = course.mapNotNull { it.getString("color") }.toSet()
                    val availableColor = colorPalette.firstOrNull { it !in usedColors }
                        ?: colorPalette[course.size() % colorPalette.size]

                    // Data to store for the new course
                    val courseData = mapOf(
                        "progress" to 0f,
                        "color" to availableColor
                    )

                    // Check and remove placeholder if it exists
                    coursesRef.document("placeholder").get().addOnSuccessListener { placeholderDoc ->
                        if (placeholderDoc.exists()) {
                            coursesRef.document("placeholder").delete()
                                .addOnSuccessListener {
                                    Log.d("CourseCreation", "Placeholder removed")
                                }
                                .addOnFailureListener {
                                    Log.e("CourseCreation", "Failed to delete placeholder", it)
                                }
                        }

                        // Save the new course data
                        courseRef.set(courseData).addOnSuccessListener {
                            Toast.makeText(context, "Course added", Toast.LENGTH_SHORT).show()
                            onCourseAdded()
                            // Reset internal course tracking state in preferences
                            val prefs = context.getSharedPreferences("AdPrefs", Context.MODE_PRIVATE)
                            prefs.edit().putInt("numberOfCourses", 0).apply()
                        }.addOnFailureListener {
                            Toast.makeText(context, "Failed to add course", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null) // Closes dialog
            .create()

        // Automatically hide keyboard if user taps outside EditText
        dialog.setOnShowListener {
            // The root view of the dialog's window
            val root = dialog.window?.decorView?.rootView
            root?.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val focusedView = dialog.currentFocus
                    if (focusedView is EditText) {
                        // Create a rectangle representing the visible bounds of the EditText
                        val outRect = Rect()
                        focusedView.getGlobalVisibleRect(outRect)
                        // Check if the touch happened outside the EditText's bounds
                        if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                            // Remove focus from the EditText
                            focusedView.clearFocus()
                            // Hide the soft keyboard
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

package com.example.groupproject

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CourseCreation(private val context: Context, private val onCourseAdded: () -> Unit) {
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.add_course, null)
        val inputField = dialogView.findViewById<EditText>(R.id.courseName)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Add New Course")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val courseName = inputField.text.toString().trim()
                if (courseName.isEmpty()) {
                    Toast.makeText(context, "Course name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val db = FirebaseFirestore.getInstance()
                val courseRef = db.collection("users").document(userId).collection("courses").document(courseName)

                courseRef.set(mapOf("progress" to 0f)).addOnSuccessListener {
                    Toast.makeText(context, "Course added", Toast.LENGTH_SHORT).show()
                    onCourseAdded()
                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to add course", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

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

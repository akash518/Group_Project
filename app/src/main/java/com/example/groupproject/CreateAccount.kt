package com.example.groupproject

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateAccount : AppCompatActivity() {
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button
    private lateinit var togglePasswordButton: ImageButton
    private var isPasswordVisible = false

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)

        emailInput = findViewById(R.id.email)
        passwordInput = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signUpButton)
        togglePasswordButton = findViewById(R.id.togglePasswordVisibility)

        val prefs = getSharedPreferences("TaskTrackerPrefs", MODE_PRIVATE)
        val savedEmail = prefs.getString("email", null)
        val savedPassword = prefs.getString("password", null)

        if (auth.currentUser == null && savedEmail != null && savedPassword != null) {
            auth.signInWithEmailAndPassword(savedEmail, savedPassword)
                .addOnSuccessListener {
                    prefs.edit().putBoolean("accountSetupComplete", true).apply()
                    openHome()
                }
                .addOnFailureListener {
                    Log.d("LoginActivity", "Auto-login failed")
                }
        }
        else if (auth.currentUser != null) {
            val userId = auth.currentUser?.uid ?: return
            val userRef = db.collection("users").document(userId)

            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    prefs.edit().putBoolean("accountSetupComplete", true).apply()
                    openHome()
                } else {
                    handleInvalidUser()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error checking user info", Toast.LENGTH_SHORT).show()
            }
        }
        else {
            loginButton.setOnClickListener {
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        saveCredentials(email, password)
                        openHome()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    }
            }

            signUpButton.setOnClickListener {
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        val userId = auth.currentUser?.uid ?: return@addOnSuccessListener
                        val userEmail = mapOf("email" to email)
                        val userRef = db.collection("users").document(userId)

                        userRef.set(userEmail)
                            .addOnSuccessListener {
                                userRef.collection("courses").document("placeholder").set(mapOf("placeholder" to true))
                                saveCredentials(email, password)
                                openHome()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to set up user", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Sign-up failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            togglePasswordButton.setOnClickListener {
                isPasswordVisible = !isPasswordVisible
                if (isPasswordVisible) {
                    passwordInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    passwordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                passwordInput.setSelection(passwordInput.text.length)
            }
        }
    }

    private fun saveCredentials(email: String, password: String) {
        val prefs = getSharedPreferences("TaskTrackerPrefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("email", email)
            putString("password", password)
            putBoolean("accountSetupComplete", true)
            apply()
        }
    }

    private fun handleInvalidUser() {
        Toast.makeText(this, "Account Removed. Please sign up again.", Toast.LENGTH_SHORT).show()

        FirebaseAuth.getInstance().signOut()
        val prefs = getSharedPreferences("TaskTrackerPrefs", MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun openHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
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
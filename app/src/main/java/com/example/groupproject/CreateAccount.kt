package com.example.groupproject

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentContainer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A full-screen DialogFragment used for login and sign-up functionality.
 * Automatically signs in if stored credentials exist or shows the login UI.
 * @param onSuccess Callback to invoke once login or sign-up completes successfully.
 */
class CreateAccount(private val onSuccess: () -> Unit) : DialogFragment() {
    // UI components
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button
    private lateinit var togglePasswordButton: ImageButton
    private var isPasswordVisible = false

    // Firebase components
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /**
     * Inflates and sets up the login/sign-up view.
     * Handles auto-login, user creation, and toggling password visibility.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.login_page, container, false)
        // Make the dialog background transparent
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        emailInput = view.findViewById(R.id.email)
        passwordInput = view.findViewById(R.id.password)
        loginButton = view.findViewById(R.id.loginButton)
        signUpButton = view.findViewById(R.id.signUpButton)
        togglePasswordButton = view.findViewById(R.id.togglePasswordVisibility)

        val prefs = requireContext().getSharedPreferences("TaskTrackerPrefs", Context.MODE_PRIVATE)
        val savedEmail = prefs.getString("email", null)
        val savedPassword = prefs.getString("password", null)
        Log.d("LoginActivity", savedEmail.toString())
        Log.d("LoginActivity", savedPassword.toString())

        // Try auto-login if saved credentials exist and user is not currently logged in
        if (auth.currentUser == null && savedEmail != null && savedPassword != null) {
            auth.signInWithEmailAndPassword(savedEmail, savedPassword)
                .addOnSuccessListener {
                    prefs.edit().putBoolean("accountSetupComplete", true).apply()
                    dismiss()
                    onSuccess()
                }
                .addOnFailureListener {
                    Log.d("LoginActivity", "Auto-login failed")
                }
        } else if (auth.currentUser != null) {
            // User already logged in – verify user document exists in Firestore
            val userId = auth.currentUser?.uid ?: return view
            val userRef = db.collection("users").document(userId)

            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    prefs.edit().putBoolean("accountSetupComplete", true).apply()
                    dismiss()
                    onSuccess()
                } else {
                    // Firebase user exists, but Firestore document is missing
                    handleInvalidUser()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Error checking user info", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Manual login/signup logic
            loginButton.setOnClickListener {
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter both email and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Sign in with email and password provided by user
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        saveCredentials(email, password)
                        dismiss()
                        onSuccess()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Invalid email or password", Toast.LENGTH_SHORT).show()
                    }
            }

            signUpButton.setOnClickListener {
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(requireContext(), "Enter email and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        val userId = auth.currentUser?.uid ?: return@addOnSuccessListener
                        val userEmail = mapOf("email" to email)
                        val userRef = db.collection("users").document(userId)

                        // Create Firestore user doc and placeholder course collection
                        userRef.set(userEmail)
                            .addOnSuccessListener {
                                userRef.collection("courses").document("placeholder").set(mapOf("placeholder" to true))
                                saveCredentials(email, password)
                                Toast.makeText(requireContext(), "Sign up Successful", Toast.LENGTH_SHORT).show()
                                dismiss()
                                onSuccess()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to set up user", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Sign-up failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            // Toggle password visibility (eye icon)
            togglePasswordButton.setOnClickListener {
                isPasswordVisible = !isPasswordVisible
                if (isPasswordVisible) {
                    passwordInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    passwordInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                passwordInput.setSelection(passwordInput.text.length) // keep cursor at end
            }
        }
        return view
    }

    /**
     * Stores email/password and account setup flag in SharedPreferences.
     */
    private fun saveCredentials(email: String, password: String) {
        val prefs = requireContext().getSharedPreferences("TaskTrackerPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("email", email)
            putString("password", password)
            putBoolean("accountSetupComplete", true)
            apply()
        }
    }

    /**
     * Called if Firebase user exists but Firestore document is missing.
     * Logs the user out and clears saved credentials.
     */
    private fun handleInvalidUser() {
        Toast.makeText(requireContext(), "Account Removed. Please sign up again.", Toast.LENGTH_SHORT).show()

        FirebaseAuth.getInstance().signOut()
        val prefs = requireContext().getSharedPreferences("TaskTrackerPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Makes the dialog fill the entire screen.
     */
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
}
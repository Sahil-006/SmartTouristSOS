package com.example.smarttouristsos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnSignup: Button = findViewById(R.id.btnSignup)
        val tvGoToLogin: TextView = findViewById(R.id.tvGoToLogin)
        val progressBar: ProgressBar = findViewById(R.id.progressBarSignup)

        val etName: EditText = findViewById(R.id.etSignupName)
        val etUsername: EditText = findViewById(R.id.etSignupUsername)
        val etEmail: EditText = findViewById(R.id.etSignupEmail)
        val etPassword: EditText = findViewById(R.id.etSignupPassword)
        val etAadhaar: EditText = findViewById(R.id.etSignupAadhaar)
        val etTripDuration: EditText = findViewById(R.id.etSignupTripDuration)

        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val aadhaar = etAadhaar.text.toString().trim()
            val tripDuration = etTripDuration.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty() || username.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSignup.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    btnSignup.isEnabled = true

                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: ""
                        val user = hashMapOf(
                            "name" to name,
                            "username" to username,
                            "email" to email,
                            "aadhaar" to aadhaar,
                            "tripDuration" to tripDuration
                        )

                        db.collection("users").document(userId)
                            .set(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Welcome to Smart Tourist SOS!", Toast.LENGTH_SHORT).show()
                                // --- THIS IS THE CHANGED PART ---
                                // Redirect directly to the main dashboard
                                val intent = Intent(this, MainActivity::class.java)
                                // Clear the back stack so the user can't go back to the login/signup screens
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish() // Close the signup activity
                                // --- END OF CHANGE ---
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvGoToLogin.setOnClickListener {
            finish() // Just close this screen to go back to the login screen
        }
    }
}


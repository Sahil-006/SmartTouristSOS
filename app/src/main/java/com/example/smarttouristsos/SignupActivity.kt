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

            // --- THIS IS THE NEW LOGIC ---
            // Step 1: Check if the username is already taken in the database
            db.collection("users").whereEqualTo("username", username).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (task.result != null && !task.result.isEmpty) {
                            // Username already exists
                            progressBar.visibility = View.GONE
                            btnSignup.isEnabled = true
                            Toast.makeText(this, "Username already exists. Please choose another.", Toast.LENGTH_LONG).show()
                        } else {
                            // Username is unique, proceed with account creation
                            // Step 2: If unique, create the user with email and password
                            createUserAccount(name, username, email, password, aadhaar, tripDuration, progressBar, btnSignup)
                        }
                    } else {
                        // Handle error during the check
                        progressBar.visibility = View.GONE
                        btnSignup.isEnabled = true
                        Toast.makeText(this, "Error checking username: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            // --- END OF NEW LOGIC ---
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun createUserAccount(name: String, username: String, email: String, password: String, aadhaar: String, tripDuration: String, progressBar: ProgressBar, btnSignup: Button) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
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
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            btnSignup.isEnabled = true
                            Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    progressBar.visibility = View.GONE
                    btnSignup.isEnabled = true
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}


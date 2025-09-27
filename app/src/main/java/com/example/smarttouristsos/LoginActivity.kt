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

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val btnLogin: Button = findViewById(R.id.btnLogin)
        val tvGoToSignup: TextView = findViewById(R.id.tvGoToSignup)
        // --- THIS IS THE CORRECTED PART ---
        val etEmail: EditText = findViewById(R.id.etLoginEmail)
        val etPassword: EditText = findViewById(R.id.etLoginPassword)
        val progressBar: ProgressBar = findViewById(R.id.progressBarLogin)
        // --- END OF CORRECTION ---

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show the loading indicator and disable the button
            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    // Hide loading and re-enable button when done
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true

                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvGoToSignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }
}


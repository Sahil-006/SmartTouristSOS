package com.example.smarttouristsos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    // Launcher for picking a contact
    private val pickContactLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)

            contactUri?.let {
                contentResolver.query(it, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val number = cursor.getString(numberIndex)
                        saveEmergencyContact(number)
                    }
                }
            }
        }
    }

    // Launcher for the READ_CONTACTS permission
    private val requestContactsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            launchContactPicker()
        } else {
            Toast.makeText(this, "Permission to read contacts is required.", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher for the POST_NOTIFICATIONS permission
    private val requestNotificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // Notification permission granted, now check for location
            checkLocationPermission()
        } else {
            Toast.makeText(this, "Notification permission is required.", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher for LOCATION permission
    private val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // Location permission granted, now check for SMS
            checkSmsPermission()
        } else {
            Toast.makeText(this, "Location permission is required.", Toast.LENGTH_LONG).show()
        }
    }

    // --- NEW LAUNCHER FOR SMS PERMISSION ---
    private val requestSmsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // All permissions granted, start the service
            startSosService()
        } else {
            Toast.makeText(this, "SMS permission is required.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPickContact: Button = findViewById(R.id.btnPickContact)
        val btnEnableSOS: Button = findViewById(R.id.btnEnableSOS)
        val btnDisableSOS: Button = findViewById(R.id.btnDisableSOS)

        btnPickContact.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED -> launchContactPicker()
                else -> requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }

        btnEnableSOS.setOnClickListener {
            // Start the chain of permission requests
            checkNotificationPermission()
        }

        btnDisableSOS.setOnClickListener {
            val intent = Intent(this, EmergencyService::class.java)
            stopService(intent)
            Toast.makeText(this, "SOS Service Disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    checkLocationPermission() // Already have notification permission, check next
                }
                else -> requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            checkLocationPermission() // Older Android, no notification permission needed
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                checkSmsPermission() // Already have location permission, check next
            }
            else -> requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // --- NEW FUNCTION TO CHECK SMS PERMISSION ---
    private fun checkSmsPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED -> {
                startSosService() // All permissions granted, start service
            }
            else -> requestSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    private fun startSosService() {
        val intent = Intent(this, EmergencyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "SOS Service Enabled", Toast.LENGTH_SHORT).show()
    }

    private fun launchContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        pickContactLauncher.launch(intent)
    }

    private fun saveEmergencyContact(number: String) {
        val sharedPref = getSharedPreferences("SOS_App_Prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("emergency_contact_number", number)
            apply()
        }
        Toast.makeText(this, "Emergency Contact Saved: $number", Toast.LENGTH_LONG).show()
    }
}
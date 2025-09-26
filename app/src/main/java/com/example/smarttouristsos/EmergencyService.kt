package com.example.smarttouristsos

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

class EmergencyService : Service() {

    private val CHANNEL_ID = "EmergencyServiceChannel"
    private var screenOnOffReceiver: BroadcastReceiver? = null
    private var pressCount = 0
    private var lastPressTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d("EmergencyService", "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        Log.d("EmergencyService", "Service Started")

        // Register the receiver to listen for screen on/off events
        if (screenOnOffReceiver == null) {
            screenOnOffReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == Intent.ACTION_SCREEN_OFF || intent?.action == Intent.ACTION_SCREEN_ON) {
                        handlePowerButtonPress()
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenOnOffReceiver, filter)
        }
        return START_STICKY
    }

    private fun handlePowerButtonPress() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPressTime < 5000) { // 3 presses within 5 seconds
            pressCount++
        } else {
            pressCount = 1 // Reset if time gap is too long
        }
        lastPressTime = currentTime
        Log.d("EmergencyService", "Power button pressed. Count: $pressCount")

        if (pressCount >= 3) {
            Log.d("EmergencyService", "SOS Triggered!")
            pressCount = 0 // Reset count after triggering
            triggerSOS()
        }
    }

    private fun triggerSOS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("EmergencyService", "SOS Failed: Missing Location or SMS permission.")
            return
        }

        val sharedPref = getSharedPreferences("SOS_App_Prefs", MODE_PRIVATE)
        val emergencyContact = sharedPref.getString("emergency_contact_number", null)

        if (emergencyContact.isNullOrEmpty()) {
            Log.d("EmergencyService", "SOS Failed: No emergency contact saved.")
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    // --- THIS IS THE LINE THAT WAS FIXED ---
                    val message = "SOS! I need help. My current location is: http://maps.google.com/maps?q=$lat,$lon"
                    sendSms(emergencyContact, message)
                } else {
                    Log.d("EmergencyService", "SOS Failed: Could not get location.")
                }
            }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("EmergencyService", "SOS SMS sent successfully to $phoneNumber")
        } catch (e: Exception) {
            Log.e("EmergencyService", "Error sending SMS", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenOnOffReceiver)
        screenOnOffReceiver = null
        Log.d("EmergencyService", "Service Destroyed")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Emergency SOS Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SOS Service Active")
            .setContentText("Listening for emergency trigger.")
            .setSmallIcon(R.drawable.ic_sos_notification)
            .build()
    }
}
package com.example.smarttouristsos

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class EmergencyService : Service() {

    private val CHANNEL_ID = "EmergencyServiceChannel"
    private val WARNING_CHANNEL_ID = "WarningChannel" // New channel for warnings
    private var screenOnOffReceiver: BroadcastReceiver? = null
    private var pressCount = 0
    private var lastPressTime: Long = 0
    private var isInsideRedZone = false // Flag to prevent spamming warnings

    // --- NEW LOCATION LISTENING LOGIC ---
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                Log.d("EmergencyService", "Current Location: Lat ${location.latitude}, Lon ${location.longitude}")
                checkRedZone(location)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createWarningNotificationChannel() // Create the new channel
        Log.d("EmergencyService", "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        Log.d("EmergencyService", "Service Started")

        registerPowerButtonReceiver()
        startLocationUpdates() // Start listening for location continuously

        return START_STICKY
    }

    private fun registerPowerButtonReceiver() {
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
    }

    // --- NEW METHOD TO START CONTINUOUS LOCATION UPDATES ---
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10 seconds
            .setMinUpdateIntervalMillis(5000) // 5 seconds
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    // --- NEW METHOD TO CHECK IF USER IS IN A RED ZONE ---
    private fun checkRedZone(currentLocation: Location) {
        val sharedPref = getSharedPreferences("SOS_App_Prefs", MODE_PRIVATE)
        val lat = sharedPref.getFloat("red_zone_lat", -1f)
        val lon = sharedPref.getFloat("red_zone_lon", -1f)
        val radius = sharedPref.getFloat("red_zone_radius", -1f)

        if (lat != -1f && lon != -1f && radius != -1f) {
            val redZoneCenter = Location("RedZoneCenter").apply {
                latitude = lat.toDouble()
                longitude = lon.toDouble()
            }
            val distance = currentLocation.distanceTo(redZoneCenter)

            if (distance < radius) {
                if (!isInsideRedZone) {
                    // User just entered the zone, send a warning
                    sendRedZoneWarning()
                    isInsideRedZone = true
                }
            } else {
                isInsideRedZone = false // User has left the zone, reset the flag
            }
        }
    }

    // --- NEW METHOD TO SEND A WARNING NOTIFICATION ---
    private fun sendRedZoneWarning() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, WARNING_CHANNEL_ID)
            .setContentTitle("⚠️ Safety Warning ⚠️")
            .setContentText("You have entered a designated unsafe area.")
            .setSmallIcon(R.drawable.ic_sos_notification)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2, notification) // Use a different ID (2) for this notification
    }

    private fun handlePowerButtonPress() {
        // (This code is unchanged)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPressTime < 5000) {
            pressCount++
        } else {
            pressCount = 1
        }
        lastPressTime = currentTime
        Log.d("EmergencyService", "Power button pressed. Count: $pressCount")

        if (pressCount >= 3) {
            Log.d("EmergencyService", "SOS Triggered!")
            pressCount = 0
            triggerSOS()
        }
    }

    private fun triggerSOS() {
        // (This code is unchanged, but I've replaced Toasts with Logs for better background behavior)
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

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    val message = "SOS! I need help. My current location is: https://www.google.com/maps/search/?api=1&query=$lat,$lon"
                    sendSms(emergencyContact, message)
                } else {
                    Log.d("EmergencyService", "SOS Failed: Could not get location.")
                }
            }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        // (This code is unchanged)
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
        fusedLocationClient.removeLocationUpdates(locationCallback) // Stop location updates
        Log.d("EmergencyService", "Service Destroyed")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Emergency SOS Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    // --- NEW NOTIFICATION CHANNEL FOR WARNINGS ---
    private fun createWarningNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(WARNING_CHANNEL_ID, "Safety Warnings", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
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
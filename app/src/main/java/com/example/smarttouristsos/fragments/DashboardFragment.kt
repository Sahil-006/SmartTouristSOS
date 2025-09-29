package com.example.smarttouristsos.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.smarttouristsos.EmergencyService
import com.example.smarttouristsos.MapActivity
import com.example.smarttouristsos.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class DashboardFragment : Fragment() {

    // --- NEW: Define a constant for our SharedPreferences file and key ---
    private val PREFS_NAME = "SOS_App_Prefs"
    private val SOS_SERVICE_ACTIVE_KEY = "sos_service_active"

    // Permission and activity launchers (unchanged)
    private val pickContactLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            contactUri?.let {
                requireActivity().contentResolver.query(it, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val number = cursor.getString(numberIndex)
                        saveEmergencyContact(number)
                    }
                }
            }
        }
    }
    private val requestContactsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) launchContactPicker() else showToast("Permission to read contacts is required.") }
    private val requestNotificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) checkForegroundServicePermission() else showToast("Notification permission is required.") }
    private val requestForegroundServicePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) checkLocationPermission() else showToast("Foreground Service permission is required.") }
    private val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) checkSmsPermission() else showToast("Location permission is required.") }
    private val requestSmsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> if (isGranted) startSosService() else showToast("SMS permission is required.") }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sosSwitch: SwitchMaterial = view.findViewById(R.id.sos_switch)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnPickContact: MaterialButton = view.findViewById(R.id.btn_set_emergency_contact)
        val btnDefineRedZone: MaterialButton = view.findViewById(R.id.btn_define_red_zone)

        // --- MODIFIED: Restore the switch state when the view is created ---
        updateUiFromSavedState(sosSwitch, tvStatus)

        // Set click listeners
        btnPickContact.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED -> launchContactPicker()
                else -> requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }

        btnDefineRedZone.setOnClickListener {
            startActivity(Intent(requireActivity(), MapActivity::class.java))
        }

        sosSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // The permission chain will now only start when the user manually toggles the switch
                checkNotificationPermission()
            } else {
                stopSosService()
            }
        }
    }

    // --- NEW FUNCTION: To read SharedPreferences and update the UI ---
    private fun updateUiFromSavedState(sosSwitch: SwitchMaterial, tvStatus: TextView) {
        val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isServiceActive = sharedPref.getBoolean(SOS_SERVICE_ACTIVE_KEY, false)

        // Set the switch's state without triggering the setOnCheckedChangeListener
        sosSwitch.isChecked = isServiceActive
        if (isServiceActive) {
            tvStatus.text = getString(R.string.sos_status_active)
        } else {
            tvStatus.text = getString(R.string.sos_status_inactive)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> checkForegroundServicePermission()
                else -> requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            checkForegroundServicePermission()
        }
    }

    private fun checkForegroundServicePermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED -> checkLocationPermission()
            else -> requestForegroundServicePermissionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE)
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> checkSmsPermission()
            else -> requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun checkSmsPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED -> startSosService()
            else -> requestSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    private fun startSosService() {
        val intent = Intent(requireActivity(), EmergencyService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().startForegroundService(intent)
        } else {
            requireActivity().startService(intent)
        }
        showToast("SOS Service Enabled")
        // --- NEW: Save the active state ---
        saveServiceState(true)
    }

    private fun stopSosService() {
        val intent = Intent(requireActivity(), EmergencyService::class.java)
        requireActivity().stopService(intent)
        showToast("SOS Service Disabled")
        // --- NEW: Save the inactive state ---
        saveServiceState(false)
        // We also need to manually update the UI text here, as the listener is what calls this function.
        view?.findViewById<TextView>(R.id.tvStatus)?.text = getString(R.string.sos_status_inactive)
    }

    // --- NEW FUNCTION: To save the service state to SharedPreferences ---
    private fun saveServiceState(isActive: Boolean) {
        val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit {
            putBoolean(SOS_SERVICE_ACTIVE_KEY, isActive)
        }
    }

    private fun launchContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        pickContactLauncher.launch(intent)
    }

    private fun saveEmergencyContact(number: String) {
        val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit {
            putString("emergency_contact_number", number)
        }
        showToast("Emergency Contact Saved: $number")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}


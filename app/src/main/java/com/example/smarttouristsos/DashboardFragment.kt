package com.example.smarttouristsos

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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial

class DashboardFragment : Fragment() {

    // --- All our permission and activity launchers from MainActivity are moved here ---
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

    private val requestContactsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchContactPicker() else showToast("Permission to read contacts is required.")
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) checkLocationPermission() else showToast("Notification permission is required.")
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) checkSmsPermission() else showToast("Location permission is required.")
    }

    private val requestSmsPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startSosService() else showToast("SMS permission is required.")
    }

    // --- onCreateView is where the layout is inflated ---
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    // --- onViewCreated is where we add all our logic ---
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find all the UI elements from our new layout
        val sosSwitch: SwitchMaterial = view.findViewById(R.id.sos_switch)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnPickContact: Button = view.findViewById(R.id.btnPickContact)
        val btnDefineRedZone: Button = view.findViewById(R.id.btnDefineRedZone)

        // --- All our button logic is moved here ---
        btnPickContact.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED -> launchContactPicker()
                else -> requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }

        btnDefineRedZone.setOnClickListener {
            val intent = Intent(requireActivity(), MapActivity::class.java)
            startActivity(intent)
        }

        sosSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                tvStatus.text = "SOS Protection is ACTIVE"
                // Start the full permission chain before enabling the service
                checkNotificationPermission()
            } else {
                tvStatus.text = "SOS Protection is INACTIVE"
                stopSosService()
            }
        }
    }

    // --- All our helper functions are moved here ---
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> checkLocationPermission()
                else -> requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            checkLocationPermission()
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
    }

    private fun stopSosService() {
        val intent = Intent(requireActivity(), EmergencyService::class.java)
        requireActivity().stopService(intent)
        showToast("SOS Service Disabled")
    }

    private fun launchContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        pickContactLauncher.launch(intent)
    }

    private fun saveEmergencyContact(number: String) {
        val sharedPref = requireActivity().getSharedPreferences("SOS_App_Prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("emergency_contact_number", number)
            apply()
        }
        showToast("Emergency Contact Saved: $number")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
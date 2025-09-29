package com.example.smarttouristsos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarttouristsos.adapters.ChatAdapter
import com.example.smarttouristsos.models.Message
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class GroupChatActivity : AppCompatActivity() {

    // --- Variables for location ---
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    // Existing variables
    private lateinit var mapView: MapView
    private var groupId: String? = null
    private var groupName: String? = null
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<Message>()
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_group_chat)

        groupId = intent.getStringExtra("GROUP_ID")
        groupName = intent.getStringExtra("GROUP_NAME")

        if (groupId == null) {
            Toast.makeText(this, "Error: Group ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        supportActionBar?.title = groupName

        // Initialize views
        mapView = findViewById(R.id.mapView)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        // Setup UI
        setupMap()
        setupChatRecyclerView()
        listenForMessages()

        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) sendMessage(messageText)
        }

        // Start the permission check process
        checkLocationPermissions()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        val mapController = mapView.controller
        mapController.setZoom(15.0)
        val startPoint = GeoPoint(12.9716, 77.5946) // Default start point
        mapController.setCenter(startPoint)
    }

    private fun checkLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, permissions[1]) != PackageManager.PERMISSION_GRANTED) {
            // If we don't have permission, request it
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // We already have permission, so set up the location overlay
            setupLocationOverlay()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, set up the location overlay
                setupLocationOverlay()
            } else {
                // Permission was denied, show a message
                Toast.makeText(this, "Location permission is required to see your position on the map.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupLocationOverlay() {
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        mapView.overlays.add(myLocationOverlay)
        mapView.invalidate()
        Toast.makeText(this, "Finding your location...", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    // --- Chat functions (unchanged) ---
    private fun setupChatRecyclerView() {
        chatAdapter = ChatAdapter(messageList)
        chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GroupChatActivity)
            adapter = chatAdapter
        }
    }

    private fun sendMessage(text: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val message = Message(text = text, senderId = currentUserId)
        db.collection("groups").document(groupId!!)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                messageEditText.text.clear()
                Log.d("GroupChatActivity", "Message sent successfully!")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show()
                Log.e("GroupChatActivity", "Error sending message", e)
            }
    }

    private fun listenForMessages() {
        db.collection("groups").document(groupId!!)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("GroupChatActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    chatAdapter.clearMessages()
                    for (doc in snapshots) {
                        val message = doc.toObject(Message::class.java)
                        chatAdapter.addMessage(message)
                    }
                    chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
    }
}


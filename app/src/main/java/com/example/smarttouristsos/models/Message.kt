package com.example.smarttouristsos.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a single chat message.
 * The empty default values are required for Firestore's automatic data mapping.
 */
data class Message(
    val text: String = "",
    val senderId: String = "",
    @ServerTimestamp
    val timestamp: Date? = null // Firestore will automatically set the time
)

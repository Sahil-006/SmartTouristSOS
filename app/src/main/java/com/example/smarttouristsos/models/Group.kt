package com.example.smarttouristsos.models

import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude
import java.util.Date

data class Group(
    var id: String = "",
    var name: String = "",
    var members: List<String> = listOf(),
    @ServerTimestamp
    val createdAt: Date? = null
) {
    // This tells Firestore to ignore this helper function when reading/writing data
    @get:Exclude
    val memberCount: Int
        get() = members.size
}
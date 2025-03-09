package com.example.terramaster

data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val chatRoomId: String = "" // Add chatRoomId with a default value
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", 0L, "")
}
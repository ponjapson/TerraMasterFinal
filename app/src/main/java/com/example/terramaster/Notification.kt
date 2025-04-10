package com.example.terramaster

import com.google.firebase.Timestamp

data class Notification(
    var notificationId: String? = null,
    val bookingId: String? = null,
    val status: String? = null, // Added status field
    val message: String? = null,
    val recipientId: String? = null,
    val senderId: String? = null,
    val timestamp: Timestamp? = null,
    val type: String? = null,
    val redirectToPayment: Boolean = false,
    var profilePicture: String? = null
)
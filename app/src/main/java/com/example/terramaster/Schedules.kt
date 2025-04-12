package com.example.terramaster

import com.google.firebase.Timestamp

data class Schedules (
    val userName: String,
    val profileImageUrl: String?,
    var startDateTime: Timestamp? = null,
    var documentStatus: String = "",
    var bookingId: String = "",
    val isProcessor: Boolean
)
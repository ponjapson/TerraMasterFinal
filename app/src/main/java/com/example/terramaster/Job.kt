package com.example.terramaster

import com.google.firebase.Timestamp

data class Job(
    val bookingId: String = "",
    val bookedUserId: String = "",
    val landOwnerUserId: String = "",
    val contractPrice: Double = 0.0,
    val downpayment: Double = 0.0,
    val startDateTime: Timestamp? = null,
    var status: String = "",
    val timestamp: Timestamp? = null,
    var stage: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var address: String = "",
    var pdfFileName: String? = null,
    var pdfUrl: String = ""
)
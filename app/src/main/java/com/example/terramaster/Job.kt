package com.example.terramaster

import com.google.firebase.Timestamp

data class Job(
    val bookingId: String = "",
    val bookedUserId: String = "",
    val landOwnerUserId: String = "",
    var contractPrice: Double = 0.0,
    var downpayment: Double = 0.0,
    val startDateTime: Timestamp? = null,
    var status: String = "",
    val timestamp: Timestamp? = null,
    var stage: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var address: String = "",
    var pdfFileName: String? = null,
    var pdfUrl: String = "",
    var age: String = "",
    var tinNumber: String = "",
    var propertyType: String = "",
    var purposeOfSurvey: String = ""

)
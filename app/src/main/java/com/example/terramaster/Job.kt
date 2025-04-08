package com.example.terramaster

import com.google.firebase.Timestamp

data class Job(
    val bookingId: String = "",
    val bookedUserId: String = "",
    val landOwnerUserId: String = "",
    var contractPrice: Double = 0.0,
    var downpayment: Double = 0.0,
    var startDateTime: Timestamp? = null,
    var status: String = "",
    val timestamp: Timestamp? = null,
    var stage: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var address: String = "",
    var pdfFileName: String? = null,
    var pdfUrl: String = "",
    var age: String = "",
    var tinNumber: String = "",
    var propertyType: String = "",
    var purposeOfSurvey: String = "",
    var contactNumber: String = "",
    var emailAddress: String = ""

)
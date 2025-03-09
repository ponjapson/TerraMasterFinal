package com.example.yourapp

data class Suggested(
    val firstName: String,
    val lastName: String,
    val userType: String,
    val address: String,
    val profileImage: String,
    val distance: Double,
    val surveyorLat: Double,
    val surveyorLon: Double,
    val userId: String
)

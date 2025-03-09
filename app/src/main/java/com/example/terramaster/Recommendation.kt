package com.example.terramaster


data class Recommendation (

    val firstName: String,
    val lastName: String,
    val userType: String,
    val address: String,
    val profileImage: String,
    val distance: Double,
    val surveyorLat: Double,
    val surveyorLon: Double,
    val userId: String,
    val ratings: Double
)